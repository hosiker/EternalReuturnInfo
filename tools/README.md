# tools

## purge_passwords.py

Firebase에 평문으로 저장된 비밀번호 필드를 삭제해요. 기본은 dry-run이라 `--apply` 없이는 아무것도 지우지 않아요.

### 준비

1. Firebase 콘솔 → 프로젝트 설정 → 서비스 계정 → **새 비공개 키 생성**으로 JSON을 받아요.
2. 그 JSON은 **이 저장소 밖**에 두세요. (예: `~/secrets/er-admin.json`) 저장소 안에 있으면 스크립트가 중단돼요.
3. 설치:

```bash
python -m venv .venv && source .venv/bin/activate
pip install firebase-admin
```

### 실행

먼저 조회만 해요.

```bash
python tools/purge_passwords.py --key ~/secrets/er-admin.json --db-url https://eternalreturninfo-4dc4b-default-rtdb.firebaseio.com
```

개수가 예상과 맞으면 삭제해요.

```bash
python tools/purge_passwords.py --key ~/secrets/er-admin.json --db-url https://eternalreturninfo-4dc4b-default-rtdb.firebaseio.com --apply
```

마지막에 다시 조회해서 `남은 항목 ... 0개 / 0개`가 나오면 끝이에요.

### 끝난 뒤

- 서비스 계정 키는 콘솔(서비스 계정 → 키 관리, 또는 Google Cloud 콘솔 IAM → 서비스 계정 → 키)에서 **삭제**하고, 로컬 JSON도 지워요.
- 관리자 SDK는 보안 규칙을 무시하고 접근해요. 규칙을 잠가둔 상태에서도 동작해요.
