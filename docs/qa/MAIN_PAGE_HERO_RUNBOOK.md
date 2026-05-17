# Main Page Hero Banner — Runbook

`HomePage` 상단 `HeroEqBanner` (§1) 가 실제로 동작하는지 검증한다.

연관 PR: `ae7234e` (Spotify app token) → `72b8537` (hero-track endpoint) → `64ec3d0` (frontend banner).

## 사전 조건

1. API 서버 재시작 (`ae7234e` 이후 빌드 필요 — Spotify app-token + hero-track 컨트롤러)
2. 웹 dev 서버 재시작 또는 강제 새로고침 (`64ec3d0` 반영)
3. EMS acquisition pool 에 `preview_url IS NOT NULL` 인 track 가 최소 1건 있어야 함. 없으면 hero 영역은 보이지 않음 (의도된 동작 — collapse).

## 실행

1. `https://imapplepie20.tplinkdns.com/` 메인 페이지 접속.
2. 페이지 상단에 hero 배너가 표시되는지 확인.
3. DevTools Console 을 열어 둔 채로 아래 단계 진행.

## 검증 단계

### 1. 데이터 표시

- [ ] 배너에 cover 이미지 + 트랙 타이틀 + 아티스트 + (있다면) 앨범 + source label (e.g. "Editorial Pick") 표시됨
- [ ] Network 탭 → `GET /api/v1/main-page/hero-track` 응답이 200 + JSON
  - 비로그인 상태에서도 200 이어야 정상 (EMS pool fallback 동작)
  - 응답에 `preview_url` 이 `https://p.scdn.co/...` 형태인지 확인

실패 시:
- 404/500 → API 서버 로그 확인, 컨트롤러 매핑 확인
- 204 → DB 에 preview_url 있는 acquisition_pool 트랙이 0건. 다음 acquisition run 까지 기다리거나 시드 데이터 점검.

### 2. 무음 자동 재생 + 시각화

- [ ] 페이지 진입 직후 audio 가 무음으로 재생 시작 (소리는 안 나도 됨)
- [ ] bars 가 움직이기 시작 (1~2초 안에)
- [ ] Console 에 hero/preview 관련 에러 없음

실패 시 — Console / Network 확인:
- `preview fetch failed: HTTP 0` 또는 CORS 에러 → Spotify p.scdn.co 가 CORS 헤더 안 주는 경우 → backend proxy 추가 필요 (별도 PR)
- `decodeAudioData FAILED` → 받았는데 디코드 불가. mp3 가 아닌 다른 컨테이너일 가능성
- bars 가 0 유지 → analyser `mode` 확인. devtools 에서:
  ```js
  // BarsVisualizer 가 받는 props 추적은 React DevTools 필요. 빠른 진단:
  document.querySelector('audio')?.paused
  document.querySelector('audio')?.currentTime
  ```
  audio 가 paused=false 이고 currentTime 이 진행되는데 bars 가 0 이면 fetch+decode 실패. Network 탭 확인.

### 3. ▶ 클릭으로 unmute

- [ ] 우측 "Listen preview" 버튼 클릭 시 사운드 시작
- [ ] 우측 상단 `sound on` 라벨로 변경
- [ ] bars 가 더 활발히 움직임 (muted 와 동일하게 보일 수 있음 — 정상)

### 4. preview 종료 → CTA

- [ ] 약 30초 후 audio 자동 종료
- [ ] "전체 듣기" 버튼 (로그인 사용자) 또는 "로그인하고 전체 듣기" 링크 (비로그인) 가 노출됨
- [ ] "Replay preview" 도 함께 노출됨 — 클릭 시 처음부터 재생

### 5. 전체 듣기 CTA 동작

로그인 사용자 only:

- [ ] "전체 듣기" 클릭
- [ ] 페이지 하단 `PlaybackDock` 가 활성화되며 트랙이 dock 에서 재생되기 시작
- [ ] hero 의 preview 는 자동 정지
- [ ] track 이 Spotify 인 경우 Spotify Web Playback SDK 로, TIDAL 인 경우 TIDAL 스트림으로 재생

실패 시:
- Spotify 트랙인데 사용자가 Spotify 연결 안 했음 → "PlatformReconnectRequired" 등의 에러. `/platforms` 에서 연결 확인.
- TIDAL 트랙인데 사용자 TIDAL 연결 없음 → 같음.

비로그인:

- [ ] "로그인하고 전체 듣기" 클릭 → `/signin` 으로 이동

## 알려진 한계

- §6 멜론 차트, §2 최신곡, §3 인기 플레이리스트, §4 알고리즘 소개, §5 GMS 추천 섹션은 미구현 (별도 PR 들).
- Spotify Client Credentials 으로 preview_url 을 후보 보강하는 흐름은 미구현 (snapshot 의 trackId 가 ems_collected_track 에 없거나 preview_url 이 null 인 경우 → fallback 동작). 보강은 follow-up PR.
- preview audio 와 dock 재생의 동시성: 사용자가 hero 를 unmute 한 상태에서 dock 으로 다른 트랙을 재생하면 둘 다 들리는 경우 발생 가능. 명시적 정지 로직 미구현.

## 한 줄 요약

`hero 트랙 표시 → 무음 bars 움직임 → ▶ unmute → 30s 후 CTA → 전체 재생` 의 5개 단계가 모두 통과되면 §1 출시 가능.
