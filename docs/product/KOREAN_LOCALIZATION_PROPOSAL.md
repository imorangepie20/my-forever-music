# Korean Localization Proposal

작성일: 2026-05-18

## 1. 목적

`my-forever-music`의 사용자 화면을 한국어 중심 경험으로 전환한다.

현재 웹앱은 핵심 제품 구조는 `PMS / EMS / GMS`로 잡혀 있지만, 화면 문구는 템플릿성 영어, 개발자용 영어, 일부 한국어가 섞여 있다. 한글화의 목표는 단순 번역이 아니라 사용자가 서비스를 "내 음악을 모으고, 추천받고, 듣는 곳"으로 자연스럽게 이해하게 만드는 것이다.

## 2. 기본 방향

- 기본 언어는 한국어로 둔다.
- `PMS`, `EMS`, `GMS` 같은 제품 도메인 약어는 유지하되, 첫 노출에는 한국어 설명을 붙인다.
- Spotify, TIDAL, FLO, Last.fm, YouTube, FastAPI, Spring Boot 같은 고유명사와 기술명은 번역하지 않는다.
- 사용자 화면은 자연스러운 한국어를 우선한다.
- 운영자 화면은 정확성과 상태 확인성을 우선한다.
- 에러는 숨기지 않고, 사용자가 다음 행동을 판단할 수 있게 원인과 조치 방향을 짧게 제공한다.

## 3. 말투 원칙

### 사용자 화면

부드럽고 명확한 문장형을 쓴다.

- 좋은 예: `내 플레이리스트를 가져와 음악 취향을 만들어요.`
- 좋은 예: `추천 후보를 확인하고 마음에 드는 곡을 저장하세요.`
- 피할 예: `Candidate evaluation signals`
- 피할 예: `Import source readiness workflow`

### 관리자 화면

상태, 주기, 실패 원인을 정확히 드러낸다.

- 좋은 예: `최근 EMS 수집 작업`
- 좋은 예: `실패한 항목만 다시 실행`
- 피할 예: `문제가 있었어요`

### 버튼

동사를 앞세운 짧은 명령형을 쓴다.

- `연결하기`
- `가져오기`
- `저장`
- `재생`
- `다시 실행`
- `삭제`
- `자세히 보기`

## 4. 용어 제안

| 현재 표현 | 한국어 제안 | 비고 |
| --- | --- | --- |
| Overview | 홈 | 사이드바에서는 짧게 사용 |
| Control Room | 서비스 현황 | 메인 대시보드 문맥 |
| Platforms | 플랫폼 연결 | 사용자가 할 일을 드러냄 |
| Platform Intake | 플랫폼 연결 준비 | 온보딩 문맥 |
| PMS Library | 내 음악 보관함 | PMS 약어와 병기 가능 |
| EMS Model | 음악 탐색 풀 | EMS 약어와 병기 가능 |
| GMS Playlists | 추천 플레이리스트 | 사용자에게 가장 직관적 |
| GMS Approval | 추천 검토 | 후보 승인보다 부드러움 |
| Playback | 플레이어 테스트 | 실제 사용자 메뉴에서는 숨김 검토 |
| Schedules | 스케줄 관리 | 관리자 메뉴 |
| EMS Acquire | EMS 수집 | 관리자 메뉴 |
| EMS Pool | EMS 큐 | 관리자 메뉴 |
| Playlist Quality | 추천 품질 | 관리자 메뉴 |
| Feature Coverage | 특성 커버리지 | 관리자 메뉴 |
| Metadata Normalize | 메타데이터 정규화 | 관리자 메뉴 |
| Sign In | 로그인 | 전역 |
| Sign Up | 회원가입 | 전역 |
| Sign Out | 로그아웃 | 전역 |
| Save Changes | 변경사항 저장 | 설정 화면 |
| Continue Onboarding | 이어서 설정하기 | 온보딩 |
| Open Platform Intake | 플랫폼 연결 열기 | CTA |
| Open GMS Preview | 추천 검토 열기 | CTA |

## 5. 도메인 용어 기준

### PMS

첫 노출: `PMS, 내 음악 보관함`

짧은 반복 노출: `PMS`

설명 문장:

`PMS는 내가 가져오고 저장한 플레이리스트와 곡, 좋아요, 재생 기록을 모아두는 개인 음악 공간입니다.`

### EMS

첫 노출: `EMS, 외부 음악 탐색 풀`

짧은 반복 노출: `EMS`

설명 문장:

`EMS는 FLO, Spotify, TIDAL, 음악 매거진 등 외부 출처에서 모은 공개 플레이리스트와 트랙 풀입니다.`

### GMS

첫 노출: `GMS, 추천 게이트웨이`

짧은 반복 노출: `GMS`

설명 문장:

`GMS는 EMS 후보 중 내 취향 모델을 통과한 추천 결과를 검토하고 저장하는 공간입니다.`

## 6. 화면별 우선순위

### 1순위: 실제 사용자 여정

가장 먼저 한글화한다.

1. 공통 레이아웃
   - Sidebar
   - Header
   - PlaybackDock
   - 공통 버튼, 빈 상태, 에러 상태

2. 메인 페이지
   - HeroEqBanner
   - LatestTracksSection
   - PopularPlaylistsSection
   - AlgorithmIntroSection
   - GmsRecommendedPlaylistsSection
   - MelonHot100Section
   - MagazineSection

3. 온보딩과 플랫폼 연결
   - Signup
   - Login
   - PlatformsPage
   - OAuth authorize/callback

4. 핵심 음악 공간
   - PmsPage
   - PmsPlaylistDetailPage
   - EmsPage
   - EmsPlaylistDetailPage
   - GmsPlaylistsPage
   - GmsPreviewPage
   - RecommendationAlgorithmPage
   - VisualizerPage

### 2순위: 운영자 화면

사용자 화면 이후 진행한다. 영어 기술 용어는 필요한 만큼 유지하되, 상태와 액션은 한국어로 바꾼다.

1. SchedulingAdminPage
2. EmsAcquisitionAdminPage
3. EmsPoolAdminPage
4. FeatureCoverageAdminPage
5. PlaylistQualityAdminPage
6. SasrecModelAdminPage
7. MetadataNormalizationAdminPage

### 3순위: 테스트/격리 화면

일반 사용자에게 숨기거나 관리자 전용으로 둘 수 있는 화면이다.

1. PlaybackHarnessPage
2. TidalPlaylistPlaybackTestPage
3. 기타 템플릿 잔여 화면
   - Calendar
   - Gallery
   - Pricing
   - Products
   - ScrumBoard
   - Widgets

## 7. 구현 제안

### 7-1. 1차 방식

한국어를 기본값으로 직접 교체한다.

이 프로젝트는 아직 다국어 지원 자체가 제품 핵심이 아니므로, 처음부터 복잡한 i18n 런타임을 도입하지 않는다. 먼저 사용자 화면의 영어 문구를 한국어로 바꾸고, 반복 문구만 작은 copy map으로 분리한다.

권장 위치:

- `apps/web/src/copy/navigation.ts`
- `apps/web/src/copy/common.ts`
- `apps/web/src/copy/playback.ts`
- `apps/web/src/copy/recommendation.ts`

### 7-2. 2차 방식

영어/한국어 전환이 필요해질 때 `i18next` 또는 동등한 lightweight i18n 레이어를 도입한다.

도입 조건:

- 영어 UI를 다시 공식 지원해야 함
- 설정 화면의 Language 항목이 실제 사용자 기능이 됨
- API 에러 메시지까지 locale별로 내려야 함

도입 전에는 Settings의 `Language` 메뉴를 실제 기능처럼 노출하지 않거나, `준비 중` 상태로 둔다.

## 8. API 메시지 기준

Backend에서 내려오는 메시지는 두 종류로 나눈다.

### 사용자 노출 메시지

한국어로 제공한다.

예:

- `플랫폼 연결이 만료되었습니다. 다시 연결해 주세요.`
- `이 플레이리스트에는 아직 재생 가능한 트랙이 없습니다.`
- `추천을 만들려면 먼저 플레이리스트를 가져와야 합니다.`

### 개발/운영 로그 메시지

영어 유지 가능.

예:

- `EMS acquisition scheduled run failed`
- `TIDAL playback target resolve failed`

로그는 운영자가 검색하기 쉬워야 하므로 기존 영어 메시지를 무리하게 바꾸지 않는다.

## 9. 주요 카피 초안

### 메인 헤더

현재:

`My Forever Music Control Room`

제안:

`내 음악 추천 홈`

보조문:

`플레이리스트를 모으고, 취향을 학습하고, 오늘 들을 음악을 추천받으세요.`

### 플랫폼 연결

현재:

`Choose the streaming source that will feed PMS and define how audio features will be enriched.`

제안:

`사용 중인 스트리밍 플랫폼을 연결해 내 음악 보관함을 채우세요.`

### PMS

현재:

`Import playlists, play library tracks, and collect approved GMS saves.`

제안:

`가져온 플레이리스트와 저장한 추천곡을 한곳에서 관리하세요.`

### EMS

현재:

`Evaluate candidates against the PMS library and listening signals.`

제안:

`외부 플레이리스트와 트렌드에서 새로운 추천 후보를 찾습니다.`

### GMS

현재:

`Review generated candidates, approve saves, and feed PMS learning events.`

제안:

`내 취향에 맞게 걸러진 추천을 확인하고 마음에 드는 곡을 저장하세요.`

### 재생 실패

제안:

`이 곡은 현재 선택한 플랫폼에서 재생할 수 없습니다. 다음 곡을 준비하고 있어요.`

관리자 상세:

`재생 대상 resolve 실패: provider=%s, status=%s`

## 10. 번역하지 않을 항목

- PMS / EMS / GMS
- Spotify / TIDAL / FLO / Last.fm / YouTube / Melon
- API / OAuth / PKCE / ISRC / BPM
- Spring Boot / FastAPI / React / Vite
- SASRec
- URL, route, env var, DB table, code identifier

단, 사용자 화면에서 기술 용어가 꼭 필요하지 않으면 숨기거나 한국어 설명으로 바꾼다.

## 11. 검수 기준

한글화 PR은 아래 조건을 만족해야 한다.

1. 핵심 사용자 여정에서 영어 문장이 보이지 않는다.
2. 버튼은 짧고 행동이 명확하다.
3. 같은 개념은 같은 번역을 쓴다.
4. 모바일 폭에서 긴 한국어 문장이 버튼이나 카드 밖으로 넘치지 않는다.
5. 오류 메시지는 원인과 다음 행동을 숨기지 않는다.
6. 기술/운영자 화면은 정확성을 해치지 않는 범위에서만 자연어화한다.
7. 브랜드명과 도메인 약어는 임의 번역하지 않는다.

## 12. 작업 순서 제안

1. 네비게이션/헤더/로그인 상태 한글화
2. 메인 페이지 섹션 한글화
3. 플랫폼 연결/로그인/회원가입 한글화
4. PMS/EMS/GMS 핵심 화면 한글화
5. 공통 플레이어와 재생 상태 문구 한글화
6. 관리자 화면 한글화
7. 남은 템플릿 페이지 정리 또는 숨김 처리
8. Playwright 또는 브라우저 수동 확인으로 긴 문구 overflow 점검

## 13. 결정 필요 항목

1. `PMS / EMS / GMS`를 사이드바에서 그대로 쓸지, 한국어 이름을 앞세울지 결정해야 한다.
   - 추천: `내 음악(PMS)`, `음악 탐색(EMS)`, `추천(GMS)`

2. 관리자 메뉴를 일반 사용자 사이드바와 완전히 분리할지 결정해야 한다.
   - 추천: 현재처럼 admin email 기준 노출하되, 한글화 이후 `관리` 섹션으로 명확히 나눈다.

3. Settings의 `Language`를 실제 기능으로 만들지 결정해야 한다.
   - 추천: 이번 단계에서는 한국어 기본값만 적용하고, Language 전환 기능은 보류한다.

4. 기존 템플릿 잔여 페이지를 한글화할지 삭제/숨김 처리할지 결정해야 한다.
   - 추천: 제품 흐름과 무관한 템플릿 페이지는 숨김 또는 제거를 우선 검토한다.
