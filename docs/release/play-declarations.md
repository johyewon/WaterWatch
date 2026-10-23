# Play Console 선언 답변 초안

Play Console → 앱 콘텐츠 에서 답하는 항목들이다. 콘솔의 항목명은 바뀔 수 있으니 제출할 때 맞춰 본다.

## 개인정보처리방침

- URL: `https://johyewon.github.io/WaterWatch/release/privacy-policy.html`
- GitHub Pages 를 켠 뒤 접속되는지 확인하고 넣는다.
- 앱의 `PermissionsRationaleActivity` 본문과 내용이 같아야 한다.

## 앱 액세스

- 선택: **모든 기능을 특별한 액세스 권한 없이 이용할 수 있음** (로그인 없음)
- 심사 중 Health Connect 관련 문의가 오면 아래 문구로 답한다.

> 로그인이 필요 없습니다. 처음 실행하면 Health Connect 수분 섭취 권한을 요청합니다. Android 13 이하 기기에서는 Health Connect 앱을 설치해야 합니다. 워치가 없어도 휴대폰 앱만으로 모든 기능을 이용할 수 있습니다. Wear OS 앱은 휴대폰 앱과 연결된 상태에서 동작하며, 워치에서 기록한 양은 휴대폰의 Health Connect 에 저장됩니다.

## 광고

- 광고 포함: **아니요**

## 콘텐츠 등급

- 카테고리: 그 외 모든 앱 유형
- 폭력, 성적 콘텐츠, 비속어, 약물, 도박, 사용자 간 소통, 위치 공유, 디지털 상품 구매: 모두 **아니요**
- 예상 등급: 전체이용가

## 타겟층 및 콘텐츠

- 대상 연령: **13세 이상** (13–15, 16–17, 18세 이상)
- 13세 미만을 고르면 가족 정책이 적용되므로 제외한다.
- 아동의 관심을 끄는 앱인가: 아니요

## 데이터 보안

### 권장 답변: 수집·공유하는 데이터 없음

- "앱이 필수 사용자 데이터 유형을 수집하거나 공유하나요?" → **아니요**

근거

- Play 기준에서 "수집"은 앱이 데이터를 기기 밖으로 전송하는 것이다. 기기 안에서만 처리하는 데이터는 공개 대상이 아니다.
- 수분 섭취 기록은 휴대폰의 Health Connect 에만 저장된다. 개발자 서버가 없다.
- 워치와 휴대폰 사이 전송은 Google Play 서비스의 Wearable API 가 사용자 소유 기기끼리 처리한다. 개발자 서버를 거치지 않는다.
- 광고·분석·크래시 리포팅 SDK 가 없다.

### 제출 전 확인할 것

- [ ] 워치↔폰 전송이 Play 기준의 "기기 밖 전송"에 해당하는지. Play Console 고객센터의 데이터 보안 섹션 도움말에서 수집 예외 항목을 확인한다.
- 해당한다고 판단되면 아래처럼 답한다.
  - 데이터 유형: 건강 및 피트니스 → 건강 정보
  - 수집 여부: 예 / 공유: 아니요
  - 목적: 앱 기능
  - 전송 중 암호화: 예
  - 처리 방식: 사용자가 기록할 때만 전송 (선택 사항)
  - 삭제 요청: Health Connect 설정에서 삭제 가능

## 건강 앱 선언

- 건강 기능 있음: **예**
- 기능 카테고리: 영양·수분 섭취 관련 항목. 정확한 항목명은 콘솔에서 확인한다.
- Health Connect 사용: **예**

### Health Connect 권한별 사유

콘솔 입력은 영어로 하는 편이 안전하다. 한국어는 내부 확인용이다.

#### `READ_HYDRATION`

- KO: 오늘 0시부터 지금까지의 수분 섭취 총량을 집계해 휴대폰과 워치 화면에 보여준다. 사용자가 다른 앱에서 기록한 양도 합산하려고 Health Connect 집계 API 를 쓴다.
- EN: Reads hydration records to show the user's total water intake for today, from midnight to now, on the phone and on the paired Wear OS watch. The app uses Health Connect aggregation so that intake logged by other apps is included in the total.

#### `WRITE_HYDRATION`

- KO: 사용자가 휴대폰이나 워치에서 "+250 ml" 버튼을 누르면 마신 양을 수분 섭취 기록으로 저장한다.
- EN: Writes a hydration record when the user taps "+250 ml" on the phone or on the watch to log a glass of water.

#### `READ_HEALTH_DATA_IN_BACKGROUND`

- KO: 사용자가 워치에서 기록하면 휴대폰 앱은 화면에 떠 있지 않은 상태에서 요청을 받아 기록을 저장한다. 그 뒤 워치에 표시되는 오늘 총량을 갱신하려면 백그라운드에서 총량을 다시 읽어야 한다. 백그라운드 읽기는 워치 기록 직후에만 일어나고, 수분 섭취 데이터만 읽는다.
- EN: When the user logs water on the watch, the phone app receives the request while it is not in the foreground and writes the record. It then needs to re-read today's hydration total in the background to update the total shown on the watch. Background reads happen only right after a log from the watch and only for hydration data.

### 백그라운드 권한이 거절되면

- 이 권한은 앱의 필수 권한이 아니다 (`HealthConnectDataSource.REQUESTED_PERMISSIONS` 참고). 권한이 없어도 기록은 저장되고, 워치에 보이는 총량만 갱신되지 않는다.
- 거절되면 매니페스트와 요청 목록에서 빼고 출시한 뒤, 워치 총량을 갱신할 다른 방법을 찾는다. 예를 들어 폰 앱이 포그라운드일 때만 총량을 발행하고, 워치는 직전 총량에 방금 기록한 양을 더해서 보여주는 방법이 있다.

## 그 밖의 선언

- 뉴스 앱: 아니요
- 정부 앱: 아니요
- 금융 기능: 없음
