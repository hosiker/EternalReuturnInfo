# Firebase 보안 규칙

| 파일 | 설명 |
|---|---|
| `firestore.rules` | Firestore 규칙 사본. 콘솔 > Firestore Database > 규칙에 게시한다. |
| `database.rules.json` | Realtime Database 규칙 사본. 콘솔 > Realtime Database > 규칙에 게시한다. |
| `rtdb.test.mjs` | Realtime Database 규칙 테스트 (Firebase 에뮬레이터, 실제 프로젝트에 연결하지 않는다). |

콘솔에 게시한 규칙과 이 폴더의 파일은 항상 같아야 한다. 규칙을 고치면 파일을 먼저 고치고 테스트를 통과시킨 뒤 게시한다.

## 테스트 실행 (Node.js 18+ 와 Java 가 필요하다)

```bash
cd firebase
npm install
npm run test:rtdb
```

## 알려진 한계
- 글 작성자는 자기 글 아래의 다른 사람 댓글을 수정할 수 없다. (노드 단위 쓰기 권한이 하위로 번지는 문제를 필드 단위 규칙으로 막았다.)
- `user` 노드는 로그인한 모든 사용자가 읽을 수 있다. 듀오 찾기, 채팅 목록, 작성자 표시에 필요하기 때문이다. 이 노드에는 민감한 값(이메일 등)을 저장하지 않는 것이 원칙이다.
- Firestore 규칙은 아직 자동 테스트가 없다.
