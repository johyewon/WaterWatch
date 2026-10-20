# 출시 체크리스트

Google Play 첫 출시까지 해야 할 일을 모은다. 항목별 상세 내용은 링크한 문서에 있다.

- [개인정보처리방침](privacy-policy.md)
- [면책 고지](disclaimer.md)
- [Play Console 선언 답변 초안](play-declarations.md)
- [스토어 등록정보](store-listing.md)
- 앱 이름: 꽥

## 0. 계정

- [ ] Play Console 개발자 계정 유형 확인 (개인 / 조직)
  - 2023-11-13 이후 만든 개인 계정이면 **테스터 12명 이상이 14일 연속 참여한 비공개 테스트**를 통과해야 프로덕션 출시를 신청할 수 있다. 일정을 최소 2주 확보한다.
  - 조직 계정이 의무인 건 의료 앱·연구 앱이다. 이 앱은 해당하지 않는 것으로 판단했고, 건강 앱 선언 때 다시 확인한다.
- [ ] 지원용 이메일 준비. 스토어에 공개되므로 개인 메일과 분리한다.
- [ ] 테스터 12명 이상 모집. Google 계정이 필요하고, 워치 보유자가 있으면 좋다.

## 1. 코드

- [ ] `wear` 매니페스트에 `com.google.android.wearable.standalone` meta-data 추가
  - 워치 앱은 폰 앱 없이 기록할 수 없으므로 `false`
- [ ] `versionCode` 번호대 분리 (예: 폰 `1xxxx`, 워치 `2xxxx`)
  - 폰과 워치가 같은 applicationId 를 쓰기 때문에 같은 번호를 올릴 수 없다. 지금은 둘 다 `1`이다.
- [ ] applicationId `com.hanix.waterwatch` 확정
  - 첫 업로드 후에는 바꿀 수 없다. 앱 이름이 바뀌어도 패키지명은 그대로 남는다.
- [ ] `app`, `wear` 두 모듈의 `app_name` 을 "꽥"으로 변경
- [ ] 디자인 시안의 "오늘의 물" 문구(권한 안내 화면 등)를 "꽥"으로 바꿔서 구현
- [ ] `PermissionsRationaleActivity` 에 개인정보처리방침 본문 표시
  - Health Connect 권한 화면의 방침 링크가 이 화면으로 온다. 스토어에 등록한 방침과 내용이 같아야 한다.
  - 앱에 INTERNET 권한이 없으므로 WebView 로 URL 을 띄우지 말고 본문을 텍스트로 넣는다.
- [ ] 의료기기 아님 고지를 앱 안에 노출 ([면책 고지](disclaimer.md))
- [ ] Health Connect 가 없거나 업데이트가 필요하면 Play 스토어로 안내 (Android 13 이하)
  - 지금은 "사용할 수 없음" 문구만 보여준다. 심사 기기에서 막히지 않도록 처리하는 편이 안전하다.
- [ ] 스크린샷용 UI 정리. 지금 화면은 골격 단계라 "Health Connect" 제목과 예외 이름이 그대로 보인다.
- [ ] 디자인 기능 구현. 스토어 문구가 디자인 기준이라 실제 앱과 맞아야 한다. 구현 상태 표는 [store-listing.md](store-listing.md#디자인-기능과-구현-상태)에 있다.
- [ ] 알림을 넣으면 `POST_NOTIFICATIONS` 권한 요청 (Android 13+). 리마인더는 정확한 알람(`SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`) 없이 WorkManager 같은 방식으로 예약한다. 정확한 알람 권한은 Play 심사 대상이다.
- [ ] Health Connect 를 디자인대로 선택 기능으로 바꾸면 앱 자체 저장소가 필요하다. 이 경우 개인정보처리방침과 데이터 보안 답변도 고친다.
- [ ] 업로드 키 keystore 생성, 안전한 곳에 백업. Play App Signing 사용
- [ ] release AAB 빌드 확인 (`app`, `wear` 각각)
- [ ] (선택) R8 활성화. 지금은 `isMinifyEnabled = false`
- [ ] (선택) 오픈소스 라이선스 고지 화면

## 2. 정책 문서

- [ ] [개인정보처리방침](privacy-policy.md) 의 `{...}` 자리 채우기
- [ ] [면책 고지](disclaimer.md) 노출 위치 결정
- [ ] GitHub Pages 공개
  - 레포 Settings → Pages → Branch `main`, 폴더 `/docs`. `johyewon/WaterWatch` 관리자 권한이 필요하다.
  - 예상 URL: `https://johyewon.github.io/WaterWatch/release/privacy-policy.html`
  - Pages 는 `docs/` 전체를 공개한다. 레포가 이미 공개 상태라 노출 범위는 달라지지 않는다.
- [ ] 브라우저에서 방침 URL 접속 확인

## 3. Play Console 앱 콘텐츠

답변 초안은 [play-declarations.md](play-declarations.md) 에 있다.

- [ ] 개인정보처리방침 URL
- [ ] 앱 액세스
- [ ] 광고
- [ ] 콘텐츠 등급
- [ ] 타겟층 및 콘텐츠
- [ ] 데이터 보안
- [ ] 건강 앱 선언 + Health Connect 권한 사유
- [ ] 그 밖의 선언 (뉴스, 정부, 금융 기능 등): 모두 해당 없음

## 4. 스토어 등록정보

문구와 에셋 규격은 [store-listing.md](store-listing.md) 에 있다.

- [ ] 스토어 제목 확정 (30자 이내). 앱 이름은 "꽥"
- [ ] 짧은 설명 (80자 이내), 자세한 설명 (4,000자 이내)
- [ ] 앱 아이콘 512×512
- [ ] 그래픽 이미지 1024×500
- [ ] 폰 스크린샷 2장 이상
- [ ] Wear OS 스크린샷 1장 이상
- [ ] 카테고리: 건강/피트니스
- [ ] 연락처 이메일
- [ ] 스토어 언어 결정 (한국어만 / 영어 추가). 영어를 추가하면 방침·설명 영문판도 필요하다.
- [ ] 고급 설정 → 폼 팩터에 Wear OS 추가

## 5. 테스트 → 출시

- [ ] 내부 테스트 트랙에 폰 AAB + 워치 AAB 업로드, 사전 출시 보고서 확인
- [ ] 비공개 테스트 시작. 테스터 12명 이상이 14일 연속 참여하도록 유지한다.
- [ ] 프로덕션 액세스 신청 (테스트 관련 설문 답변)
- [ ] 프로덕션 출시 → 심사. Health Connect 권한 선언 검토가 함께 진행된다.

## 참고

- [Publish your health app on Google Play](https://developer.android.com/health-and-fitness/health-connect/publish)
- [Declare access to Health Connect data types](https://developer.android.com/health-and-fitness/guides/health-connect/publish/declare-access)
- [Wear OS 앱 패키징·배포](https://developer.android.com/training/wearables/packaging)
- [Play Console 고객센터: 건강 앱 선언](https://support.google.com/googleplay/android-developer/answer/14738291)
- [Play Console 고객센터: 조직 계정이 필요한 앱](https://support.google.com/googleplay/android-developer/answer/10788890)
