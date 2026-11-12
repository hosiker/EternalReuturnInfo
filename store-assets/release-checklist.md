# 출시 준비 점검표 (ER info)

체크하면서 진행합니다. 「직접」은 이호식 님이 해야 하는 일, 「코드」는 저장소에 이미 반영된 일입니다.

## A. 서명 키 (가장 중요, 한 번만 만들고 절대 잃어버리지 않기)
Play는 앱 서명 키를 Google이 보관하고, 개발자는 **업로드 키**로 서명해서 올립니다. 업로드 키 파일을 잃으면 Google에 재설정을 요청해야 하니 백업하세요.

1. 「직접」 키 만들기 (PowerShell, 폴더는 먼저 만들어 두세요):
   ```
   mkdir C:\dev\keys
   keytool -genkeypair -v -keystore C:\dev\keys\er-info-upload.jks -alias er-info-upload -keyalg RSA -keysize 2048 -validity 10000
   ```
   비밀번호와 이름 등을 물어봅니다. **비밀번호는 메모해서 안전한 곳에 보관**하세요. `keytool`이 없으면 Android Studio에 들어 있는 JDK의 `bin` 폴더에서 실행합니다.
2. 「직접」 `er-info-upload.jks` 파일을 **USB나 본인만 열 수 있는 클라우드 두 곳 이상에 백업**하세요. 저장소에는 절대 올리지 않습니다.
3. 「직접」 `local.properties`에 네 줄을 추가해요 (이 파일은 Git에 안 올라갑니다).
   ```
   uploadStoreFile=C\:\\dev\\keys\\er-info-upload.jks
   uploadStorePassword=키스토어 비밀번호
   uploadKeyAlias=er-info-upload
   uploadKeyPassword=키 비밀번호
   ```
4. 「코드」 `app/build.gradle.kts`가 위 네 값이 모두 있을 때 release 빌드를 이 키로 서명합니다. (없으면 서명 없는 빌드가 나옵니다)

## B. 빌드
- 「코드」 `versionCode`는 26, `versionName`은 2.0.0으로 올렸습니다. 업로드할 때마다 `versionCode`를 1 이상 올려야 합니다.
- 「직접」 **이미지 스위치가 꺼져 있는지 확인**: `local.properties`에 `useCharacterImages=true`가 있으면 지우세요. 빌드 로그 첫머리에 "실험체 이미지: 제외 (이니셜 아바타)"가 나와야 합니다.
- 「직접」 `signReleaseWithDebug=true`는 점검용이므로 **지우세요**. (둘 다 있으면 점검용 서명이 이깁니다)
- 「직접」 앱 번들(AAB) 만들기:
  ```
  .\gradlew.bat bundleRelease
  ```
  결과: `app\build\outputs\bundle\release\app-release.aab`
- 「직접」 난독화 매핑 파일 보관: `app\build\outputs\mapping\release\mapping.txt` (Play 콘솔에 함께 올리면 오류 보고를 읽을 수 있습니다)
- 16KB 정렬: AAB는 Google이 APK로 바꿔 배포하므로 업로드 후 Play 콘솔의 "앱 번들 탐색기"에서 경고가 없는지 보세요. APK를 직접 만들 때는 `zipalign -c -P 16 -v 4 app-release.apk`로 확인합니다.

## C. 설치 점검 (제출 전 마지막)
- 서명된 release APK(`assembleRelease`)를 에뮬레이터나 실제 폰에 설치해 전 화면 확인: 로그인, 홈, 듀오, 게시판(글쓰기·상세·검색), 채팅, 프로필·설정, 팝업, 알림.
- 새로 설치한 직후 가입이 되고 회원 탈퇴가 끝까지 되는지 테스트 계정으로 한 번 확인하세요.

## D. Play 콘솔 입력 (계정이 정해진 뒤)
- 스토어 등록정보: `store-assets/store-listing-ko.md`
- 데이터 보안: `store-assets/data-safety-answers.md`
- 앱 콘텐츠: 개인정보처리방침 URL, 광고 없음, 앱 액세스(검토용 테스트 계정), 콘텐츠 등급 설문, 대상 연령(16세 이상), 사용자 생성 콘텐츠(신고·차단 있음), 계정 삭제 URL
- 검토용 테스트 계정: 로그인이 필요한 앱이라 검토자가 쓸 **전용 계정(이메일·비밀번호)**을 만들어 "앱 액세스" 항목에 적습니다. 실제 이용자 계정을 쓰지 마세요.
- 국가·가격: 한국, 무료.
- 출시 방식: 먼저 내부 테스트나 비공개 테스트로 올려 확인한 뒤 프로덕션으로 올리고, 프로덕션은 **단계적 출시(예: 20% → 100%)**를 권합니다.

## E. 출시 후
- 신고 접수함을 주 2회 확인(`tools/moderation.py inbox`), 월 1회 정리(purge)
- Play 콘솔의 **비정상 종료·ANR** 화면 확인
- GitHub Actions 영상 목록 갱신(6시간마다)이 초록색인지 가끔 확인
