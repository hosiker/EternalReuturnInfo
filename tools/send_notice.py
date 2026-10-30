"""가입자에게 보안 사고 안내 메일을 한 명씩 개별 발송하는 스크립트.

  - 수신자는 Firebase Auth 의 이메일/비밀번호 계정이에요. (rotate_passwords.py 대상과 같아요)
  - 모든 수신자에게 따로 보내서 서로의 주소가 보이지 않아요.
  - 이미 보낸 주소는 로그 파일에 기록해서, 다시 실행해도 중복 발송하지 않아요.
  - 발신 계정은 ER info 전용 Gmail 이고, 앱 비밀번호를 써요. (환경변수 ER_SMTP_PASSWORD 또는 입력창)

기본은 dry-run(대상 개수와 메일 미리보기)이에요. 사용법은 tools/README.md 참고.
"""
import argparse
import getpass
import os
import smtplib
import ssl
import sys
import time
from email.message import EmailMessage
from email.utils import formataddr
from pathlib import Path

import firebase_admin
from firebase_admin import auth, credentials

from purge_passwords import abort_if_key_in_repo
from rotate_passwords import has_password_login

SMTP_HOST = "smtp.gmail.com"
SENDER_NAME = "ER info"
CONFIRM_PHRASE = "안내 메일 발송"
DEFAULT_NOTICE = Path(__file__).resolve().parent / "notice_template.txt"


def load_notice(path: Path, contact: str) -> tuple[str, str]:
    """첫 줄 '제목: ...' 을 제목으로, 나머지를 본문으로 나눈다. {contact} 는 발신 주소로 바꾼다."""
    first, _, rest = path.read_text(encoding="utf-8").partition("\n")
    if not first.startswith("제목:"):
        sys.exit(f"중단: {path} 첫 줄이 '제목: ...' 형식이어야 해요.")
    return first.removeprefix("제목:").strip(), rest.lstrip("\n").replace("{contact}", contact)


def build_message(sender: str, to: str, subject: str, body: str) -> EmailMessage:
    message = EmailMessage()
    message["From"] = formataddr((SENDER_NAME, sender))
    message["To"] = to
    message["Reply-To"] = sender
    message["Subject"] = subject
    message.set_content(body)
    return message


def open_smtp(user: str, password: str, port: int):
    """465 는 SSL, 587 은 STARTTLS 로 연결하고 로그인한 서버를 돌려준다."""
    context = ssl.create_default_context()
    if port == 465:
        server = smtplib.SMTP_SSL(SMTP_HOST, port, context=context, timeout=30)
    else:
        server = smtplib.SMTP(SMTP_HOST, port, timeout=30)
    try:
        if port != 465:
            server.starttls(context=context)
        server.login(user, password.replace(" ", ""))
    except Exception:
        server.close()
        raise
    return server


def pick_recipients(users, *, include_all: bool, only: set[str] | None) -> list[str]:
    emails = []
    for user in users:
        if not user.email or (not include_all and not has_password_login(user)):
            continue
        email = user.email.lower()
        if (only is None or email in only) and email not in emails:
            emails.append(email)
    return emails


def send_all(recipients, *, connect, build, record, sleep, interval, limit):
    """한 명씩 보내고 (성공 목록, 실패 목록)을 돌려준다. 연결이 끊기면 한 번 다시 연결한다."""
    server = connect()
    sent, failed = [], []
    for email in recipients:
        if limit is not None and len(sent) >= limit:
            break
        try:
            try:
                server.send_message(build(email))
            except smtplib.SMTPServerDisconnected:
                server = connect()
                server.send_message(build(email))
        except Exception as error:  # 수신 거부, 한도 초과 등 사유를 기록하고 계속한다
            failed.append((email, str(error)))
            continue
        record(email)
        sent.append(email)
        print(f"  [{len(sent)}] 발송 완료")
        sleep(interval)
    return sent, failed


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--smtp-user", required=True, help="발신 Gmail 주소 (ER info 전용 계정)")
    parser.add_argument("--key", type=Path, help="서비스 계정 키(JSON) 경로 (저장소 밖). --test-to 가 아니면 필수")
    parser.add_argument("--notice", type=Path, default=DEFAULT_NOTICE, help="안내문 파일 (기본: tools/notice_template.txt)")
    parser.add_argument("--apply", action="store_true", help="실제로 발송한다 (없으면 dry-run)")
    parser.add_argument("--test-to", metavar="EMAIL", help="이 주소로만 1통 보낸다 (수신자 조회 없음, 로그 기록 없음)")
    parser.add_argument("--all", action="store_true", help="Google 로그인 계정 등 이메일이 있는 모든 계정에 보낸다")
    parser.add_argument("--only", nargs="+", metavar="EMAIL", help="이 주소들에만 보낸다")
    parser.add_argument("--limit", type=int, help="최대 발송 수 (나눠서 보낼 때)")
    parser.add_argument("--interval", type=float, default=3.0, help="메일 사이 대기 초 (기본 3)")
    parser.add_argument("--port", type=int, choices=(465, 587), default=465, help="SMTP 포트: 465(SSL) 또는 587(STARTTLS)")
    args = parser.parse_args()

    subject, body = load_notice(args.notice, args.smtp_user)
    if args.test_to:
        recipients, log_path = [args.test_to.lower()], None
    else:
        if not args.key:
            sys.exit("중단: --key 가 필요해요. (테스트만 하려면 --test-to)")
        abort_if_key_in_repo(args.key)
        firebase_admin.initialize_app(credentials.Certificate(str(args.key)))
        only = {e.lower() for e in args.only} if args.only else None
        recipients = pick_recipients(auth.list_users().iterate_all(), include_all=args.all, only=only)
        log_path = args.key.resolve().parent / "notice_sent.log"  # 이메일이 들어 있어서 저장소 밖(키 옆)에 둔다
        done = set(log_path.read_text(encoding="utf-8").split()) if log_path.exists() else set()
        skipped = [e for e in recipients if e in done]
        recipients = [e for e in recipients if e not in done]
        print(f"이미 발송한 주소 {len(skipped)}개는 건너뛰어요.")

    print("모드:", "발송 (--apply)" if args.apply else "dry-run (미리보기)")
    print(f"발송 대상: {len(recipients)}개 / 발신: {args.smtp_user}\n")
    print(f"--- 제목: {subject}\n{body}\n---")
    if not args.apply:
        return

    if not args.test_to:
        if input(f"\n{len(recipients)}명에게 발송합니다. 계속하려면 '{CONFIRM_PHRASE}' 를 입력하세요: ").strip() != CONFIRM_PHRASE:
            sys.exit("취소했어요. 아무것도 보내지 않았어요.")
    password = os.environ.get("ER_SMTP_PASSWORD") or getpass.getpass("앱 비밀번호(입력이 보이지 않아요): ")

    def connect():
        return open_smtp(args.smtp_user, password, args.port)

    def record(email):
        if log_path:
            with log_path.open("a", encoding="utf-8") as log:
                log.write(email + "\n")

    try:
        sent, failed = send_all(
            recipients, connect=connect, build=lambda to: build_message(args.smtp_user, to, subject, body),
            record=record, sleep=time.sleep, interval=args.interval, limit=args.limit,
        )
    except smtplib.SMTPAuthenticationError:
        sys.exit("로그인 실패: 앱 비밀번호(16자리)와 2단계 인증 설정을 확인하세요.")
    except OSError as error:  # 연결 끊김, 시간 초과 등
        sys.exit(
            f"연결 실패: {error}\n"
            "  1) --port 587 을 붙여 다시 시도해 보세요.\n"
            "  2) 발신 계정(Gmail)의 '보안 알림' 메일에 차단된 로그인 시도가 있는지 확인하세요.\n"
            "  3) 백신/방화벽의 메일 검사 기능을 잠시 끄고 다시 시도해 보세요."
        )
    print(f"\n완료. 성공 {len(sent)}개 / 실패 {len(failed)}개")
    for email, reason in failed:
        print(f"  실패: {email} ({reason})")
    if failed:
        print("\n실패한 주소만 다시 보내려면: --apply --only " + " ".join(email for email, _ in failed))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
