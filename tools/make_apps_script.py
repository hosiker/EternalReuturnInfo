"""안내 메일을 Google Apps Script 로 보내기 위한 스크립트 파일(.gs)을 만든다.

SMTP 로그인이 막힐 때 쓰는 대안이에요. 구글 서버에서 ER info 계정으로 직접 보내서
앱 비밀번호가 필요 없어요. 수신자 목록과 안내문이 들어간 notice_sender.gs 를 키 파일 옆에 만들어요.
이 파일에는 수신자 이메일이 들어 있으니 저장소에 올리지 말고, 발송이 끝나면 Apps Script 프로젝트와 함께 지우세요.
사용법은 tools/README.md 참고.
"""
import argparse
import json
import sys
from pathlib import Path

import firebase_admin
from firebase_admin import auth, credentials

from purge_passwords import abort_if_key_in_repo
from send_notice import DEFAULT_NOTICE, load_notice, pick_recipients

TEMPLATE = """\
// 자동 생성 파일: ER info 안내 메일 발송 (Google Apps Script)
// 수신자 이메일이 들어 있어요. 발송이 끝나면 이 프로젝트를 삭제하세요.
const TEST_TO = __TEST_TO__;
const SUBJECT = __SUBJECT__;
const BODY = __BODY__;
const RECIPIENTS = __RECIPIENTS__;
const SENDER_NAME = 'ER info';
const PAUSE_MS = 1500;

// 1) 먼저 이 함수를 실행해서 테스트 메일 1통을 확인하세요.
function testSend() {
  if (!TEST_TO) throw new Error('TEST_TO 가 비어 있어요. --test-to 로 다시 만드세요.');
  send_(TEST_TO);
  console.log('테스트 메일 1통을 보냈어요: ' + TEST_TO);
}

// 2) 테스트가 정상이면 이 함수를 실행해서 전체에게 보내세요. 다시 실행해도 보낸 사람은 건너뛰어요.
function sendToAll() {
  const props = PropertiesService.getScriptProperties();
  const done = JSON.parse(props.getProperty('sent') || '[]');
  const pending = RECIPIENTS.filter(email => done.indexOf(email) < 0);
  const quota = MailApp.getRemainingDailyQuota();
  console.log('남은 대상 ' + pending.length + '명 / 오늘 남은 발송 한도 ' + quota + '명');
  const batch = pending.slice(0, quota);
  const failed = [];
  batch.forEach(email => {
    try {
      send_(email);
      done.push(email);
      props.setProperty('sent', JSON.stringify(done));
    } catch (error) {
      failed.push(email + ' (' + error.message + ')');
    }
    Utilities.sleep(PAUSE_MS);
  });
  console.log('완료. 성공 ' + (batch.length - failed.length) + '명 / 실패 ' + failed.length + '명');
  failed.forEach(line => console.log('실패: ' + line));
  const rest = pending.length - batch.length;
  if (rest > 0) console.log('발송 한도 때문에 ' + rest + '명이 남았어요. 내일 sendToAll 을 다시 실행하세요.');
}

function send_(to) {
  MailApp.sendEmail(to, SUBJECT, BODY, { name: SENDER_NAME });
}
"""


def render_script(subject: str, body: str, recipients: list[str], test_to: str | None) -> str:
    """JSON 문자열은 JavaScript 문자열 리터럴로 그대로 쓸 수 있다."""
    values = {
        "__TEST_TO__": json.dumps(test_to or "", ensure_ascii=False),
        "__SUBJECT__": json.dumps(subject, ensure_ascii=False),
        "__BODY__": json.dumps(body, ensure_ascii=False),
        "__RECIPIENTS__": json.dumps(recipients, ensure_ascii=False, indent=2),
    }
    script = TEMPLATE
    for placeholder, value in values.items():
        script = script.replace(placeholder, value)
    return script


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--key", required=True, type=Path, help="서비스 계정 키(JSON) 경로 (저장소 밖에 둘 것)")
    parser.add_argument("--contact", required=True, help="안내문에 적을 담당자 이메일 (ER info 계정)")
    parser.add_argument("--test-to", help="테스트 메일을 받을 개인 이메일")
    parser.add_argument("--notice", type=Path, default=DEFAULT_NOTICE, help="안내문 파일 (기본: tools/notice_template.txt)")
    parser.add_argument("--all", action="store_true", help="Google 로그인 계정 등 이메일이 있는 모든 계정에 보낸다")
    parser.add_argument("--only", nargs="+", metavar="EMAIL", help="이 주소들에만 보낸다")
    parser.add_argument("--out", type=Path, help="출력 파일 (기본: 키 파일 옆 notice_sender.gs)")
    args = parser.parse_args()

    abort_if_key_in_repo(args.key)
    out = args.out or args.key.resolve().parent / "notice_sender.gs"
    abort_if_key_in_repo(out)
    subject, body = load_notice(args.notice, args.contact)
    firebase_admin.initialize_app(credentials.Certificate(str(args.key)))
    only = {e.lower() for e in args.only} if args.only else None
    recipients = pick_recipients(auth.list_users().iterate_all(), include_all=args.all, only=only)
    if not recipients:
        sys.exit("수신자가 0명이에요. 만들지 않았어요.")

    out.write_text(render_script(subject, body, recipients, args.test_to), encoding="utf-8")
    print(f"만들었어요: {out}")
    print(f"수신자 {len(recipients)}명 / 제목: {subject}")
    print("이 파일에는 이메일 주소가 들어 있어요. 발송이 끝나면 지우세요.")


if __name__ == "__main__":
    main()
