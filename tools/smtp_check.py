"""Gmail SMTP 연결이 어느 단계에서 막히는지 확인하는 진단 도구.

단계: TCP 연결 -> 서버 인사 -> EHLO -> (587이면) STARTTLS -> 로그인
각 단계의 성공/실패와 의심 원인을 보여줘요. 메일은 보내지 않고, 비밀번호는 출력하지 않아요.
로그인까지 보려면 환경변수 ER_SMTP_PASSWORD 에 앱 비밀번호를 넣거나 실행 중 입력창에 입력하세요.
"""
import argparse
import getpass
import os
import smtplib
import socket
import ssl

HOST = "smtp.gmail.com"


def step(label, action) -> bool:
    try:
        action()
    except Exception as error:
        print(f"  [실패] {label} -> {type(error).__name__}: {error}")
        return False
    print(f"  [OK]   {label}")
    return True


def check_port(port: int, user: str, password: str | None) -> str:
    """단계별로 확인하고 결론 한 줄을 돌려준다."""
    print(f"\n== 포트 {port} ==")
    if not step("TCP 연결", lambda: socket.create_connection((HOST, port), timeout=15).close()):
        return "TCP 연결 자체가 막혀 있어요. 방화벽, 회사/학교망, 통신망 차단을 의심하세요."

    context = ssl.create_default_context()
    opened = {}

    def open_server():
        if port == 465:
            opened["s"] = smtplib.SMTP_SSL(HOST, port, context=context, timeout=30)
        else:
            opened["s"] = smtplib.SMTP(HOST, port, timeout=30)

    if not step("서버 인사 수신", open_server):
        return "연결 직후 서버가 끊겼어요. 백신의 메일 보호, 방화벽, 통신망이 SMTP를 가로채거나 막는 경우가 많아요."
    server = opened["s"]
    try:
        if not step("EHLO", lambda: server.ehlo()):
            return "인사 직후 끊겼어요. 위와 같은 원인을 의심하세요."
        if port == 587 and not step("STARTTLS(암호화 전환)", lambda: server.starttls(context=context)):
            return "암호화 전환에 실패했어요. SSL을 검사하는 보안 프로그램을 의심하세요."
        if password is None:
            return "연결은 정상이에요. (로그인은 건너뜀)"

        def login():
            try:
                server.login(user, password.replace(" ", ""))
            except smtplib.SMTPAuthenticationError as error:
                reply = error.smtp_error.decode(errors="replace") if isinstance(error.smtp_error, bytes) else error.smtp_error
                raise RuntimeError(f"Google 응답 {error.smtp_code}: {reply}") from None

        if step("로그인", login):
            return "모두 정상이에요. 이 포트로 발송할 수 있어요."
        return "로그인 단계에서 실패했어요. 위 메시지를 확인하세요. (끊김이면 새 계정 보호, 거부면 앱 비밀번호 문제)"
    finally:
        try:
            server.close()
        except Exception:
            pass


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--smtp-user", required=True, help="발신 Gmail 주소")
    parser.add_argument("--skip-login", action="store_true", help="로그인은 하지 않고 연결만 확인한다")
    args = parser.parse_args()

    password = None
    if not args.skip_login:
        password = os.environ.get("ER_SMTP_PASSWORD") or getpass.getpass("앱 비밀번호(입력이 보이지 않아요): ")

    results = {port: check_port(port, args.smtp_user, password) for port in (465, 587)}
    print("\n== 결론 ==")
    for port, conclusion in results.items():
        print(f"  포트 {port}: {conclusion}")


if __name__ == "__main__":
    main()
