# Music Discovery And Listening UX

작성일: `2026-05-09`

이 문서는 `my-forever-music`의 PMS / EMS / GMS 화면 구성을 사용자의 실제 목적에 맞게 정렬하기 위한 제품 구성 문서입니다.

## 1. 사용자 목적

이 프로젝트 사용자의 핵심 목적은 자신의 취향에 맞는 곡들을 쉽게 추천받고, 편하게 음악을 감상하는 것입니다.

따라서 모든 UI/UX는 아래 우선순위에 종속됩니다.

1. 사용자가 듣고 싶은 음악을 빠르게 찾는다.
2. 추천 결과가 내 취향에 맞는지 쉽게 판단한다.
3. 좋은 곡과 플레이리스트를 PMS에 저장한다.
4. 대부분의 실제 음악 감상은 PMS에서 이어진다.
5. 사용자의 평가, 저장, 재생 행동은 다시 추천 품질을 높이는 데이터가 된다.

EMS와 GMS는 최종 목적지가 아니라 PMS 감상 경험을 좋게 만들기 위한 보조 흐름입니다.

## 2. 제품 공간 역할

### PMS

PMS는 사용자의 음악 감상 중심 공간입니다.

PMS에서 사용자는 다음 행동을 주로 합니다.

- 가져온 플레이리스트를 연다.
- 트랙 목록을 보고 재생한다.
- 추천으로 저장한 곡을 듣는다.
- 개인 플레이리스트를 만든다.
- 대부분의 음악 감상을 이어간다.

PMS 화면은 검색 결과나 수집 로그보다 `내가 들을 음악`과 `현재 듣는 흐름`을 우선 노출해야 합니다.

### EMS

EMS는 서비스가 능동적으로 수집한 외부 공개 플레이리스트 풀을 평가하고, 사용자의 GMS 후보로 넘기기 위한 공간입니다.

EMS의 목적은 음악 감상 자체가 아니라 PMS와 GMS에 보낼 후보를 찾는 것입니다.

EMS 화면은 공개 플레이리스트 풀, 후보 확인, 모델 입력 요약을 제공하되, 수집 로그나 개별 트랙 저장 목록이 화면의 주인공이 되면 안 됩니다.

### GMS

GMS는 추천 후보를 최종 승인하는 공간입니다.

GMS에서 사용자는 추천 결과를 빠르게 듣고, 저장하거나 거절합니다. 승인된 곡은 PMS로 들어가고, 이후 감상은 PMS에서 이어집니다.

## 3. EMS 화면 재구성 원칙

현재 EMS 화면은 탭으로 나누지 않고, 하나의 화면에서 `Overview`와 `Public Playlist Pool`을 함께 보여줍니다.

이 결정의 목적:

- Overview LLM 해석과 deterministic 상태를 같은 콘텐츠 타일 안에서 빠르게 확인
- DB에 저장된 공개 playlist 후보를 바로 확인
- 검색/수집 로그/개별 트랙 관리 UI가 EMS 화면의 주인공이 되는 상황 방지
- playlist card -> detail tracks -> playback queue 흐름을 단순하게 유지

사용자 검색으로 공개 playlist pool을 만드는 흐름은 기본 사용자 경험에서 제외합니다.

### 3-1. Public Playlist Pool의 목적

Public Playlist Pool은 이미 EMS DB에 저장된 외부 공개 플레이리스트를 확인하는 화면입니다.

사용자는 다음 행동만 수행합니다.

- 플랫폼별 공개 플레이리스트 풀 확인
- 플레이리스트 카드 클릭 후 트랙 목록 상세 화면으로 이동
- 저장된 트랙 목록을 재생 큐로 넘김
- GMS 후보 흐름으로 들어갈 수 있는 풀의 상태 확인

### 3-2. Collected Tracks 제거

`Collected Tracks` 섹션은 EMS 화면에서 제거합니다.

이유:

- 사용자가 검색 결과의 개별 트랙 목록을 계속 관리하는 것은 피로도가 높습니다.
- 검색 결과 저장 상태가 주요 UI처럼 보입니다.
- 최종 감상은 PMS에서 해야 하므로 EMS에서 트랙 수집 목록을 크게 보여줄 필요가 없습니다.

개별 트랙 데이터는 내부 후보 데이터와 playlist detail 화면에서만 사용하고, 기본 EMS overview 화면에는 노출하지 않습니다.

### 3-3. 검색 결과 저장 금지

사용자 검색은 provider 공개 검색 결과를 미리 보는 동작입니다.

- 검색 결과는 사용자가 별도 저장/가져오기 동작을 실행하기 전까지 EMS 테이블에 넣지 않습니다.
- 검색 결과를 기본 EMS playlist pool과 섞지 않습니다.
- EMS 화면에서 표시되는 playlist와 track detail은 검색 preview가 아니라 DB에 저장된 데이터입니다.

## 4. 공개 플레이리스트 수집과 노출

공개 플레이리스트는 사용자의 검색 행동으로 만들어지지 않습니다.

서비스가 능동적으로 provider 공개 검색 API를 호출하고, 결과 playlist와 track metadata를 EMS DB에 저장합니다.

초기 수집 정책:

- 기본 수집 주기: 6시간
- 수집 기준: 운영 수집용 provider credential을 가진 user id
- 수집 대상: TIDAL, Spotify 공개 playlist
- 수집 seed query: 관리자 설정값
- 기본 노출: DB에 저장된 playlist 중 랜덤 노출
- 실패 처리: provider/token/scope/API 실패를 mock, 다른 provider, 빈 성공으로 대체하지 않음

환경변수:

- `EMS_DISCOVERY_USER_ID`: 운영 수집용 provider credential을 가진 user id
- `EMS_DISCOVERY_REFRESH_INTERVAL_MS`: 기본 `21600000`
- `EMS_DISCOVERY_SEED_QUERIES`: 기본 `jazz,indie,k-pop,electronic,soul,ambient,workout,focus`
- `EMS_DISCOVERY_PLATFORMS`: 기본 `tidal,spotify`
- `EMS_DISCOVERY_PER_QUERY_LIMIT`: 기본 `5`
- `EMS_DISCOVERY_DISPLAY_LIMIT`: 기본 `12`

## 5. Public Playlist Pool 화면 구성

EMS 화면의 공개 playlist 영역은 아래 순서로 구성합니다.

```text
EMS Page
  1. Overview
     - PMS library readiness
     - EMS pool readiness
     - GMS readiness
     - deterministic candidate direction
     - optional LLM interpretation

  2. Random EMS Pool Exposure
     - DB에 저장된 공개 playlist 랜덤 카드
     - 카드 클릭 시 트랙 목록 상세 화면으로 이동

  3. Provider Pool Status
     - TIDAL 공개 플레이리스트
     - Spotify 공개 플레이리스트
     - Apple Music 공개 플레이리스트
     - iTunes 공개 플레이리스트 또는 차트 기반 컬렉션

  4. Pool Feedback
     - 수집 실패
     - provider 인증 필요
     - provider 미지원
     - 트랙 로딩 실패
```

하단에는 플랫폼별 공개 플레이리스트 영역을 둡니다.

이 영역은 사용자가 검색어를 고민하지 않아도 탐색을 시작할 수 있게 하는 진입점입니다.

## 6. EMS Overview와 LLM 해석 레이어

EMS Overview는 사용자가 seed weight, energy, familiarity 같은 내부 모델 값을 직접 조정하는 화면이 아닙니다.

역할:

- PMS 라이브러리 준비 상태 확인
- EMS 공개 playlist pool 상태 확인
- GMS 후보 검토 가능 상태 확인
- 사용자 취향 모델 snapshot 확인
- 이번 후보 방향을 사람이 이해할 수 있는 문장으로 확인

LLM은 추천 후보를 직접 결정하지 않습니다.

```text
PMS listening/save/skip data
+ EMS public playlist pool
+ GMS approval/rejection history
        ↓
deterministic feature/score pipeline
        ↓
candidate ranking + evidence
        ↓
LLM interpretation layer
        ↓
EMS Overview / GMS explanation
```

구현 경계:

- Spring API는 deterministic context를 만든다.
- FastAPI AI service의 `/v1/ems/overview`가 LLM 해석을 담당한다.
- LLM model/API key가 설정되지 않았으면 요약을 만들지 않고 `model_not_configured` 상태를 노출한다.
- LLM이 provider 실패, DB 부재, 모델 근거 부족을 임의로 덮거나 꾸며내면 안 된다.

환경변수:

- `AI_EMS_OVERVIEW_MODEL`: EMS Overview 해석에 사용할 구체 LLM 모델
- `AI_LLM_API_KEY`: LLM provider API key
- `AI_LLM_BASE_URL`: OpenAI-compatible chat completions base URL, 기본 `https://api.openai.com/v1`
- `AI_EMS_OVERVIEW_PATH`: Spring API가 호출할 AI service path, 기본 `/v1/ems/overview`

## 7. 공개 플레이리스트 Provider 표시 기준

Public Playlist Pool에는 다음 provider를 표시합니다.

### TIDAL

역할:

- TIDAL 공개 플레이리스트 검색
- TIDAL playlist track 목록 확인
- TIDAL playback 가능 트랙이면 바로 재생
- 타 플랫폼 track이면 재생 직전에 TIDAL search로 playable track id resolve

표시 상태:

- 연결됨
- 재연결 필요
- 검색 가능하지만 재생 권한 부족
- provider 오류

### Spotify

역할:

- Spotify 공개 플레이리스트 검색
- Spotify playlist track 목록 확인
- Spotify Connect 또는 SDK 기반 재생 흐름으로 연결
- 사용자가 TIDAL을 재생 플랫폼으로 선택한 경우 track metadata로 TIDAL playback target을 resolve

표시 상태:

- 연결됨
- 재연결 필요
- scope 부족
- provider 오류

### Apple Music

역할:

- Apple Music 공개 플레이리스트 탐색 후보
- 실제 연동은 Apple Developer Program 준비 이후 구현

표시 상태:

- 준비 전
- developer token 필요
- 구현 예정

Apple Music은 준비 전이라도 UI 구조상 provider slot을 남겨둘 수 있습니다. 단, 실제 검색이 구현되지 않았는데 검색 가능한 것처럼 보이면 안 됩니다.

### iTunes

역할:

- iTunes Search API 또는 차트 기반 공개 컬렉션 탐색 후보
- Apple Music 전체 연동 전, 공개 metadata 탐색 보조 provider

표시 상태:

- 공개 metadata 검색 가능
- playback 불가
- PMS 후보 metadata로만 사용

iTunes는 스트리밍 재생 provider가 아니라 공개 metadata provider로 취급합니다.

## 8. 카드 클릭 동작

플레이리스트 카드는 어디에 있든 기본 클릭 동작이 `트랙 목록 상세 화면 이동`이어야 합니다.

적용 대상:

- PMS 플레이리스트 카드
- PMS 개인 플레이리스트 카드
- EMS 공개 풀 플레이리스트 카드
- GMS에서 참조하는 PMS 플레이리스트 카드

카드 내부 버튼은 보조 액션입니다.

- `Play`: 현재 플레이어에서 재생
- `Save`: 후보 저장
- `Open`: provider 원본 열기
- `Use`: 현재 컨텍스트로 선택

카드 클릭과 보조 버튼 클릭은 명확히 분리합니다.

## 9. PMS 중심 감상 흐름

추천과 검색은 PMS로 수렴해야 합니다.

```text
Scheduled external provider collection
  -> EMS Discovery Pool
  -> 사용자 모델이 후보 평가
  -> GMS 추천 후보로 사용
  -> playlist detail 확인
  -> GMS 승인
  -> PMS 저장
  -> PMS에서 감상
  -> 재생/저장/스킵/평가 이벤트로 사용자 모델 갱신
```

사용자가 음악을 계속 듣는 화면은 PMS입니다.

EMS Public Playlist Pool에서 모든 것을 끝내려고 하면 화면이 복잡해지고, GMS에서 추천 후보를 너무 오래 붙잡으면 감상 흐름이 끊깁니다.

## 9-1. 플랫폼 독립 재생 원칙

PMS/EMS/GMS에 표시되는 playlist와 track metadata는 DB에 저장된 값을 사용합니다.

재생은 사용자가 선택한 현재 playback platform에 맞춰 시작합니다.

- 현재 playback platform이 `tidal`이고 track에 TIDAL id가 있으면 그대로 재생합니다.
- 현재 playback platform이 `tidal`이고 track이 Spotify 등 다른 플랫폼에서 온 경우, 재생 직전에 `POST /api/v1/platforms/playback/tidal/resolve-track`으로 TIDAL playable target을 찾습니다.
- resolve 결과는 현재 queue item에만 임시로 붙이고 EMS/PMS DB에는 자동 저장하지 않습니다.
- TIDAL stream endpoint가 `FULL`이 아닌 preview manifest를 반환하면 성공처럼 처리하지 않고 provider 오류를 표시합니다.
- playback 시작 시 기존 player state를 먼저 초기화한 뒤 새 player를 띄우고, resolve/stream 준비 중에는 spinner와 준비 메시지를 보여줍니다.

## 10. 너무 단순하거나 너무 복잡하지 않기 위한 기준

### 너무 단순한 상태

- 검색 결과가 카드 몇 개만 있고 왜 추천에 쓰이는지 알 수 없다.
- 저장 후 어디로 가는지 알 수 없다.
- 플레이리스트와 트랙 목록 이동이 끊긴다.
- 재생 상태가 페이지 이동 중 사라진다.

### 너무 복잡한 상태

- 검색 결과, 저장 결과, 수집된 트랙, 모델 입력, 추천 결과가 한 화면에 모두 섞인다.
- 사용자가 직접 seed weight나 internal model 값을 조정해야 한다.
- provider별 오류와 내부 저장 상태가 사용자에게 과도하게 노출된다.
- EMS에서 음악 감상까지 모두 해결하려 한다.

### 목표 수준

- 사용자는 검색 결과를 확인하고 좋은 후보를 고를 수 있다.
- 내부 저장과 수집은 동작하되 UI의 주인공이 되지 않는다.
- PMS는 감상 중심으로 충분히 풍부하다.
- EMS와 GMS는 PMS를 더 좋게 만드는 짧고 명확한 보조 흐름이다.

## 11. 다음 구현 작업

1. EMS Public Playlist Pool의 provider별 상태 표시를 더 명확하게 만든다.
2. TIDAL / Spotify 스케줄러 수집량을 늘리고 랜덤 노출 품질을 검증한다.
3. TIDAL playback target resolve 실패 원인을 UI에서 track 단위로 확인할 수 있게 한다.
4. Apple Music / iTunes는 실제 구현 상태에 맞게 준비 전 또는 metadata-only 상태로 표시한다.
5. PMS 화면은 감상과 개인 플레이리스트 중심으로 계속 정리한다.

## 12. 구현 주의사항

- 공개 풀 수집 결과가 저장되더라도 `수집된 데이터 관리 화면`처럼 만들지 않는다.
- provider 오류를 다른 provider 결과나 mock 결과로 대체하지 않는다.
- 수집 실패와 권한 실패는 정확히 표시하되, 사용자가 불필요한 내부 정보를 조작하게 하지 않는다.
- Apple Music과 iTunes는 실제 구현 범위가 다르므로 UI에서도 역할을 분리한다.
- 사용자 모델 조정값은 사용자가 직접 만지는 UI가 아니라, 재생/저장/평가 행동으로 자동 학습되는 흐름이어야 한다.
