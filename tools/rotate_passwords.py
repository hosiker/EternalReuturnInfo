"""모든 이메일/비밀번호 계정의 비밀번호를 무작위 값으로 바꾸고, 비밀번호 재설정 메일을 보낸다.

노출된 비밀번호를 무효화하기 위한 일회성 스크립트예요.
  1. 계정마다 무작위 비밀번호로 교체하고, 기존 로그인 세션(refresh token)을 폐기해요.
  2. 바로 이어서 Firebase가 비밀번호 재설정 메일을 보내요. (한국어 템플릿)
Google 로그인 전용 계정은 건드리지 않아요.

기본은 dry-run(대상 개수만 조회)이에요. 실제 실행은 --apply 와 확인 문구 입력이 필요해요.
사용법은 tools/README.md 참고.
"""
import argparse
import json
import secrets
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

import firebase_admin
from firebase_admin import auth, credentials

from purge_passwords import abort_if_key_in_repo

SEND_URL = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key={key}"
ANDROID_PACKAGE = "com.irionna.eternalreturninfo"
SEND_INTERVAL_SEC = 1.0
CONFIRM_PHRASE = "비밀번호 무효화"
GOOGLE_SERVICES = Path(__file__).resolve().parent.parent / "app" / "google-services.json"


def load_api_key(explicit: str | None) -> str:
    if explicit:
        return explicit
    data = json.loads(GOOGLE_SERVICES.read_text(encoding="utf-8"))
    return data["client"][0]["api_key"][0]["current_key"]


def has_password_login(user) -> bool:
    return bool(user.email) and any(p.provider_id == "password" for p in user.provider_data)


def send_reset_mail(email: str, api_key: str, android_cert: str | None) -> str | None:
    """성공하면 None, 실패하면 에러 메시지를 돌려준다."""
    headers = {
        "Content-Type": "application/json",
        "X-Android-Package": ANDROID_PACKAGE,
        "X-Firebase-Locale": "ko",
    }
    if android_cert:
        headers["X-Android-Cert"] = android_cert
    body = json.dumps({"requestType": "PASSWORD_RESET", "email": email}).encode()
    request = urllib.request.Request(SEND_URL.format(key=api_key), data=body, headers=headers)
    try:
        urllib.request.urlopen(request, timeout=30).read()
        return None
    except urllib.error.HTTPError as error:
        try:
            return json.loads(error.read())["error"]["message"]
        except (ValueError, KeyError):
            return f"HTTP {error.code}"
    except urllib.error.URLError as error:
        return str(error.reason)


def process(users, *, apply, skip_rotate, only, rotate, revoke, send, sleep):
    """대상 계정을 처리하고 (대상 수, 건너뛴 수, 교체 실패, 메일 실패)를 돌려준다."""
    targets, skipped = [], 0
    for user in users:
        if not has_password_login(user) or (only and user.email.lower() not in only):
            skipped += 1
        else:
            targets.append(user)
    rotate_failed, mail_failed = [], []
    if not apply:
        return targets, skipped, rotate_failed, mail_failed
    for index, user in enumerate(targets, start=1):
        if not skip_rotate:
            try:
                rotate(user.uid, secrets.token_urlsafe(32))
                revoke(user.uid)
            except Exception as error:  # firebase_admin 예외 종류가 많아서 전부 기록하고 계속한다
                rotate_failed.append((user.email, str(error)))
                continue  # 비밀번호가 안 바뀐 계정에는 메일을 보내지 않는다
        message = send(user.email)
        if message:
            mail_failed.append((user.email, message))
        print(f"  [{index}/{len(targets)}] 처리 완료")
        sleep(SEND_INTERVAL_SEC)
    return targets, skipped, rotate_failed, mail_failed


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--key", required=True, type=Path, help="서비스 계정 키(JSON) 경로 (저장소 밖에 둘 것)")
    parser.add_argument("--api-key", help="웹 API 키 (기본: app/google-services.json 의 current_key)")
    parser.add_argument("--android-cert", help="API 키에 Android 앱 제한이 있을 때 SHA-1 (콜론 없이)")
    parser.add_argument("--apply", action="store_true", help="실제로 실행한다 (없으면 dry-run)")
    parser.add_argument("--skip-rotate", action="store_true", help="비밀번호 교체 없이 재설정 메일만 다시 보낸다")
    parser.add_argument("--only", nargs="+", metavar="EMAIL", help="이 이메일들만 처리한다 (실패한 계정 재시도용)")
    args = parser.parse_args()

    abort_if_key_in_repo(args.key)
    firebase_admin.initialize_app(credentials.Certificate(str(args.key)))
    api_key = load_api_key(args.api_key)
    only = {e.lower() for e in args.only} if args.only else None

    all_users = list(auth.list_users().iterate_all())
    targets, skipped, _, _ = process(
        all_users, apply=False, skip_rotate=args.skip_rotate, only=only,
        rotate=None, revoke=None, send=None, sleep=None,
    )
    print("모드:", "실행 (--apply)" if args.apply else "dry-run (조회만)")
    print(f"전체 계정: {len(all_users)}개 / 처리 대상(이메일·비밀번호): {len(targets)}개 / 제외: {skipped}개")
    if not args.apply:
        return

    action = "재설정 메일만 발송" if args.skip_rotate else "비밀번호 교체 + 세션 폐기 + 재설정 메일 발송"
    print(f"\n{len(targets)}개 계정에 대해 '{action}'을 실행합니다.")
    if not args.skip_rotate:
        print("교체된 계정은 재설정 메일의 링크로 새 비밀번호를 만들기 전까지 로그인할 수 없어요.")
    if input(f"계속하려면 '{CONFIRM_PHRASE}' 를 입력하세요: ").strip() != CONFIRM_PHRASE:
        sys.exit("취소했어요. 아무것도 바꾸지 않았어요.")

    _, _, rotate_failed, mail_failed = process(
        all_users, apply=True, skip_rotate=args.skip_rotate, only=only,
        rotate=lambda uid, pw: auth.update_user(uid, password=pw),
        revoke=auth.revoke_refresh_tokens,
        send=lambda email: send_reset_mail(email, api_key, args.android_cert),
        sleep=time.sleep,
    )
    print(f"\n완료. 교체 실패 {len(rotate_failed)}개 / 메일 실패 {len(mail_failed)}개")
    for email, reason in rotate_failed:
        print(f"  교체 실패: {email} ({reason})")
    for email, reason in mail_failed:
        print(f"  메일 실패: {email} ({reason})")
    if mail_failed:
        retry = " ".join(email for email, _ in mail_failed)
        print(f"\n메일만 다시 보내려면: --apply --skip-rotate --only {retry}")
    sys.exit(1 if rotate_failed or mail_failed else 0)


if __name__ == "__main__":
    main()
