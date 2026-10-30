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

## rotate_passwords.py

노출된 비밀번호를 무효화해요. 이메일/비밀번호 계정마다 ① 비밀번호를 무작위 값으로 교체하고 ② 기존 로그인 세션을 폐기한 뒤 ③ 비밀번호 재설정 메일(한국어)을 보내요. Google 로그인 전용 계정은 건드리지 않아요.

**교체된 계정은 재설정 메일의 링크로 새 비밀번호를 만들기 전까지 로그인할 수 없어요.** 아래 순서를 지켜주세요.

### 실행 전

1. 사용자에게 보낼 **안내 메일을 먼저(또는 동시에)** 보내요. 설명 없이 재설정 메일만 가면 피싱으로 오해받아요.
2. 콘솔 → Authentication → 템플릿 → 비밀번호 재설정에서 제목을 알아보기 쉽게 고쳐요. (예: `[ER info] 비밀번호를 다시 설정해 주세요`)
3. 서비스 계정 키를 새로 만들어요. (이전 키는 삭제했을 거예요.) 저장소 밖에 두세요.
4. 같은 가상환경(`firebase-admin` 설치됨)을 써요.

### 실행

먼저 대상 개수만 조회해요. 전체 계정 수와 이메일/비밀번호 계정 수를 확인하세요.

```bash
python tools/rotate_passwords.py --key ~/secrets/er-admin.json
```

맞으면 실행해요. `비밀번호 무효화` 를 직접 입력해야 시작돼요. 메일은 계정당 1초 간격으로 보내요.

```bash
python tools/rotate_passwords.py --key ~/secrets/er-admin.json --apply
```

### 문제가 생기면

- 메일 실패 계정이 나오면 마지막에 재시도 명령이 출력돼요. 그대로 실행하면 그 계정에만 메일을 다시 보내요. (`--apply --skip-rotate --only 이메일...`)
- `API_KEY_ANDROID_APP_BLOCKED` 같은 오류가 나오면 API 키에 Android 앱 제한이 걸려 있는 거예요. 콘솔 → 프로젝트 설정 → 내 앱의 **SHA-1 인증서 지문**을 콜론(:) 없이 `--android-cert` 로 넘기세요.
- `TOO_MANY_ATTEMPTS_TRY_LATER` 는 발송 한도예요. 잠시 뒤 재시도하세요.
- 끝나면 서비스 계정 키를 다시 삭제하세요.
