# 메인 페이지 — Hero 비주얼 EQ 배너 (§1) 설계

작성: 2026-05-17
연관:
- 제품 스펙: [docs/architecture/CONTENTS_MAIN_PAGE.md](CONTENTS_MAIN_PAGE.md)
- Visual EQ 신호 경로: [docs/architecture/VISUAL_EQ_PLAYER_DESIGN.md](VISUAL_EQ_PLAYER_DESIGN.md)
- 디자인 베이스: [/Users/woosungjo/music-space/imapplepie-music-player/](/Users/woosungjo/music-space/imapplepie-music-player/)

---

## 0. 결정 사항 (사용자 컨펌)

| 항목 | 선택 |
|---|---|
| **A. 곡 소스** | A1+A2 하이브리드 — 로그인 사용자: GMS top 추천 / 비로그인 또는 cold-start: Pitchfork "Best New Tracks" 최신 |
| **B. 미리듣기 음원** | Spotify `preview_url` (30s mp3) |
| **C. 자동재생** | C1 — 무음 자동 재생 + 큰 ▶ 오버레이 (클릭 시 unmute) |
| **D. preview 이후** | D2 — 30s 후 "전체 듣기" CTA → dock 으로 전환 |
| §6 멜론 | 스크래핑으로 구현. 정책 컨펌은 추후 |

---

## 1. 시스템 콘텍스트

```
HomePage
  └─ HeroEqBanner (신규)
        ├─ 곡 선택: useHeroTrack() → backend GET /api/v1/main-page/hero-track
        ├─ 음원 fetch: Spotify preview_url
        ├─ 시각화: useTidalAudioAnalyser 재활용 (이름은 hook 그대로, 내부적으로 "audio source agnostic" 로 generalize)
        └─ click → PlaybackContext.playItem(track) → dock 전환
```

신규 백엔드:
- `GET /api/v1/main-page/hero-track?user_id=...` — 로그인 시 GMS 우선, 없으면 EMS Best New Tracks 최신. 응답에 Spotify `preview_url`, cover, title, artist, track_id 포함.
- 곡이 Spotify track 으로 매핑 안 되거나 `preview_url` null 이면 다음 후보로 fallback.

---

## 2. 곡 선택 로직 (backend)

`HeroTrackService.resolve(userId?)`:

1. **로그인 + GMS 데이터 있음**: `gms_recommendation_candidate` 또는 `/gms/recommendations/preview` 의 1순위 track. preview_url 없으면 다음 후보 (5개까지 시도).
2. **비로그인 또는 GMS cold-start**: `ems_acquired_signal` 중 source name = "Pitchfork Best New Tracks" 인 최근 항목 (1주일 이내), Spotify track 매핑 + preview_url 있는 것 중 최신.
3. **2 까지 fallback 실패**: `ems_acquired_signal` 의 다른 high-weight 소스 (Stereogum, Best New Tracks 외 Pitchfork track reviews) 같은 방식.
4. **여전히 없음**: 응답 `null` — frontend 는 hero 자체를 숨김 (배너 영역 collapse).

응답 shape:
```json
{
  "track_id": "...",
  "title": "...",
  "artist_name": "...",
  "album_title": "...",
  "image_url": "https://...",
  "preview_url": "https://p.scdn.co/mp3-preview/...",
  "spotify_track_id": "...",
  "tidal_track_id": null,
  "source_label": "Pitchfork Best New Track",
  "source_url": "https://pitchfork.com/..."
}
```

### Spotify Client Credentials 흐름 (신규 백엔드 작업)

비로그인 사용자에게도 Spotify search/track lookup 으로 preview_url 을 가져오려면 사용자 토큰 없이 호출 가능해야 한다.

→ `SpotifyClientCredentialsTokenService` 신규 추가:
- env: `SPOTIFY_CLIENT_ID` (기존), `SPOTIFY_CLIENT_SECRET` (기존). 이미 있음
- POST `https://accounts.spotify.com/api/token` with `grant_type=client_credentials` → app token
- 캐시 1시간 (token expires_in 보통 3600)
- `SpotifyWebApiClient` 가 user token 없으면 이 app token 으로 fallback (search / tracks endpoint 만)

---

## 3. 프론트엔드 컴포넌트

### 3.1 신규 파일

| 파일 | 책임 |
|---|---|
| `apps/web/src/pages/HomePage.tsx` | 메인 페이지 컴포지션 (이미 존재 — 섹션들 추가) |
| `apps/web/src/components/home/HeroEqBanner.tsx` | hero 배너 컴포넌트 |
| `apps/web/src/components/home/HeroEqOverlay.tsx` | preview audio + 시각화 + ▶ overlay |
| `apps/web/src/hooks/useHeroTrack.ts` | backend fetch + 캐시 |
| `apps/web/src/hooks/usePreviewAudioAnalyser.ts` | preview audio 전용 PCM analyser (기존 `useTidalAudioAnalyser` 의 일반화 버전) |

### 3.2 hook 재사용 전략

기존 `useTidalAudioAnalyser` 는 TIDAL 의 `tidalAudioCapture` subscribe 와 HLS init-segment 디코드 로직이 박혀 있음. 분리:

- **`useTidalAudioAnalyser`** → TIDAL 전용 그대로 유지
- **신규 `usePreviewAudioAnalyser(audioUrl, audioElement)`** — Spotify preview mp3 같은 단일 URL 전용:
  1. fetch(url, { mode: 'cors' })
  2. decodeCompleteAudioData
  3. ring buffer 1회 채우기 (전체 30s)
  4. read window @ audio.currentTime
- 공통 → 이미 분리됨 (`audioRingBuffer`, `simpleFft`)

이렇게 하면 TIDAL/preview 두 source 가 독립적으로 진화 가능.

---

## 4. UI 상태 머신

```
[loading hero-track]
  ↓ resolved
[ready · muted autoplay · ▶ overlay visible]
  ↓ user clicks ▶
[playing · sound on · overlay fades · 30s progress bar]
  ↓ playback ends OR user clicks elsewhere
[finished · CTA "전체 듣기" visible]
  ↓ user clicks CTA
[전체 track 재생 시작 — PlaybackContext.playItem, dock 활성]
```

추가 케이스:
- preview fetch/decode 실패: bars 가 0 으로 정지 + "preview unavailable" 라벨 + 그대로 track 정보만 카드로 표시. 클릭 → dock 으로 전체 재생.
- 사용자가 dock 으로 다른 곡 재생 중이면 hero 의 muted autoplay 는 그대로. dock 우선.
- hero 가 "playing (unmuted)" 인 동안 다른 dock 재생이 시작되면 hero 는 자동 음소거.

---

## 5. 레이아웃

```
┌────────────────────────────────────────────────────────────┐
│                                                              │
│  [blurred cover BG]                                         │
│                                                              │
│   ┌──────────────┐    Title (large serif)                   │
│   │              │    Artist · Album                          │
│   │  cover art   │    "Pitchfork Best New Track"             │
│   │  (square)    │                                           │
│   │              │    ▶ Listen Preview (큰 버튼, accent)     │
│   └──────────────┘                                           │
│                                                              │
│   ▁▂▃▅▆▇█▇▅▃▂▁▂▃▅▆▇█  ← bars EQ (full width)              │
│                                                              │
│   ━━━━━━━━━━━━━━━━━━━━ 00:14 / 00:30                        │
└────────────────────────────────────────────────────────────┘
```

- 높이: `h-[60vh]` 정도 (반응형, 모바일은 더 짧게)
- 70%+ 이미지: 흐린 cover BG 가 전체 채움 + 좌측 square cover
- 위 비주얼라이저는 `BarsVisualizer` 그대로 재사용 (preview 전용 hook 으로 데이터만 갈아끼움)

---

## 6. 자동재생 / 음소거 처리

- `<audio>` element: `muted=true`, `autoplay=true`, `loop=false`, `playsInline=true`
- 무음 자동재생은 현행 브라우저 모두 허용
- 사용자 클릭 → `audio.muted = false`, `audio.currentTime = 0` (preview 처음부터)
- AudioContext 는 PCM 디코드 전용 (`decodeCompleteAudioData`) — 그래프 build 안 함. user gesture 불필요.
- 비주얼라이저는 muted 상태에서도 동작 (audio.currentTime + ring buffer 읽기)

---

## 7. 검증 기준

- [ ] 비로그인 진입 시 Pitchfork Best New Track 가 hero 에 표시됨
- [ ] 로그인 + GMS 추천 있는 사용자: 본인 top 추천 곡이 hero 에 표시됨
- [ ] muted 자동 재생 진행 중 bars 가 음악에 맞춰 움직임
- [ ] ▶ 클릭 시 unmute, 사운드 들림
- [ ] 30s 끝나면 "전체 듣기" CTA 표시
- [ ] CTA 클릭 → dock 활성 + 전체 track 재생 시작
- [ ] dock 에서 다른 곡 재생 시작하면 hero 자동 음소거
- [ ] preview_url 이 없는 hero-track 응답이 와도 page crash 없이 카드 형태로 fallback

---

## 8. 리스크 / 미해결

| ID | 리스크 | 대응 |
|---|---|---|
| H1 | Spotify preview CDN (`p.scdn.co`) 가 CORS 헤더 안 보내면 fetch+decode 실패 | 구현 초반 실측. 막히면 backend proxy (`/api/v1/platforms/playback/preview-proxy?url=`) 추가 — 작업 추가 1일 |
| H2 | GMS preview 가 top track 의 preview_url 까지 노출 안 함 | backend `HeroTrackService` 가 별도로 Spotify lookup 해서 보강 |
| H3 | Pitchfork "Best New Tracks" RSS item 에 Spotify track 매핑 정확도 | 기존 EMS acquisition 의 매핑 로직 재사용 — 부정확하면 다음 후보로 fallback |
| H4 | 첫 진입 시 hero-track 응답 + preview fetch + decode 가 ~2~3s 걸림 | 로딩 중 cover/title 카드만 먼저 보여주고 EQ 는 데이터 준비되면 fade-in |

---

## 9. §2~§6 섹션 — 한 줄 요약

| §  | 섹션 | 데이터 소스 | 노트 |
|---|---|---|---|
| 2 | 최신곡 | `ems_acquired_signal` 최근 1주일 top tracks | 5~10개, 카드 grid |
| 3 | 인기 플레이리스트 | `ems_collected_playlist` popularity score 정렬 | 4~6개 카드 |
| 4 | 추천 알고리즘 소개 | 신규 정적 페이지 `/about/recommendation` | marketing-style — Phase 2 가능 |
| 5 | 최신 GMS 추천 5개 | `/api/v1/gms/playlists/preview` top 5 | 로그인 필요. 비로그인엔 cold-start 안내 카드 |
| 6 | 멜론 핫 100 | 신규 스크래퍼 + 신규 테이블 `melon_chart_snapshot` | 일 1회 갱신, 정책 컨펌 추후 |

각 섹션 별 PR 분리. §2 → §3 → §5 → §6 → §4 순서 권장 (가시 효과 큰 순서).

---

## 10. PR 분리

| PR | 내용 | 예상 작업량 |
|---|---|---|
| PR-1 | Spotify Client Credentials 토큰 서비스 | 1일 |
| PR-2 | `/api/v1/main-page/hero-track` endpoint + `HeroTrackService` | 1일 |
| PR-3 | `HeroEqBanner` + `usePreviewAudioAnalyser` + HomePage 통합 | 1~2일 |
| PR-4 | §2 최신곡 섹션 | 0.5일 |
| PR-5 | §3 인기 플레이리스트 섹션 | 0.5일 |
| PR-6 | §5 GMS 추천 섹션 | 0.5일 |
| PR-7 | §6 멜론 차트 스크래퍼 + 테이블 + 섹션 | 1~2일 |
| PR-8 | §4 추천 알고리즘 소개 정적 페이지 | 0.5일 |

PR-1 ~ PR-3 가 hero 의존성. 그 다음 §2~§6 병렬 가능.

---

## 11. 명시적 결정

| 결정 | 이유 |
|---|---|
| Client Credentials 토큰 서비스 추가 | 비로그인 사용자에게도 Spotify preview 접근 |
| 기존 `useTidalAudioAnalyser` 안 건드림 | TIDAL/preview 진화 분리 |
| §6 멜론 정책 컨펌은 구현 후로 미룸 | 사용자 명시 |
| ▶ overlay (C1) | spec 의 "보여주기" 가 핵심. 음소거 EQ 만으로도 시선 끌기 충분 |
