# Visual EQ Music Player — 설계 문서

작성: 2026-05-16
연관 문서:
- 제품 스펙: [docs/product/VISUAL_EQ_MUSIC_PLAYER.md](../product/VISUAL_EQ_MUSIC_PLAYER.md)
- 디자인 베이스: [/Users/woosungjo/music-space/imapplepie-music-player/](/Users/woosungjo/music-space/imapplepie-music-player/)
- 정책: [REAL_IMPLEMENTATION_POLICY.md](REAL_IMPLEMENTATION_POLICY.md), [PLAYBACK_ERROR_HANDLING_POLICY.md](PLAYBACK_ERROR_HANDLING_POLICY.md)

---

## 0. 이 문서의 목적

`Phase 1` 의 구현 범위·인터페이스·검증 기준을 사전 합의한다.
지난 시도가 rollback 된 이유는 *오디오 신호 경로의 CORS 제약* 과 *기존 디폴트 플레이어 자동 대체* 두 가지 — 본 설계는 그 둘을 명시적으로 다룬다.

---

## 1. 범위 (Scope)

### Phase 1 (이 문서가 다루는 범위)
- TIDAL 전용 풀스크린 비주얼 EQ 플레이어 페이지 `/visualizer`
- 실제 TIDAL 오디오 신호 기반 FFT 시각화 (Chromium 계열)
- 트랙/플레이리스트 이미지 + 큐 정보 표시
- 기존 `PlaybackContext` 와 `tidalStreamPlayback` 모듈을 **읽기만 함** — 재생 제어 동일 경로
- 기존 `PlaybackDock` 는 그대로 둠, 라우트 진입 시에만 visualizer 표시

### Phase 2 (별도 PR, 본 문서 *외*)
- LLM 텍스트/이미지 생성
- 매거진 RSS 표시 (기존 `ems_acquired_signal` 재활용)
- 모바일/Safari 지원 확장
- 디폴트 플레이어 대체 옵션 (사용자 설정으로)

명시적 비목표:
- 다른 플랫폼 (Spotify/Apple/PMS/EMS/GMS) 진입 — Phase 1 외
- 새로운 acquisition 소스 추가
- TIDAL 재생 로직 수정

---

## 2. 시스템 콘텍스트

```
PlaybackContext (existing)
  ├── tidalStreamPlayback.ts: HLS.js → <audio> (detached)
  │     └── audioElement (singleton, crossOrigin unset)
  └── currentTrack / currentPlaylist / playbackState (observable)

VisualizerPage (new)
  ├── PlaybackContext 구독 (트랙 메타데이터 + 큐 + 재생 상태)
  ├── tidalStreamPlayback.getAudioElement() (신규 노출 API)
  │     └── audio.captureStream() → AnalyserNode → frequencyData
  └── Procedural fallback (captureStream 미지원 시)
```

---

## 3. 오디오 신호 경로 — 결정 사항

### 선택: B (captureStream → MediaStreamSource)

```ts
const stream = audioElement.captureStream()       // MediaStream
const ctx = new AudioContext()
const source = ctx.createMediaStreamSource(stream)
const analyser = ctx.createAnalyser()
source.connect(analyser)                          // ⚠️ destination 에는 안 연결
analyser.fftSize = 256
const data = new Uint8Array(analyser.frequencyBinCount)
analyser.getByteFrequencyData(data)               // RAF 루프에서 호출
```

### 왜 A (createMediaElementSource) 가 아닌가
- `createMediaElementSource(audio)` 는 audio element 가 CORS-clean 일 것을 요구 → `crossOrigin="anonymous"` 필요
- [tidalStreamPlayback.ts:31-33](../../apps/web/src/lib/tidalStreamPlayback.ts#L31-L33) 코멘트: TIDAL CDN CORS 설정으로 crossOrigin 설정 시 재생 자체가 깨질 수 있음
- 지난 rollback 의 직접 원인 — 같은 함정 반복 금지

### 왜 destination 에 연결하지 않는가
- `audioElement` 가 detached 인 채로 이미 재생 중 — 시스템 스피커로 출력은 진행 중
- analyser 를 `ctx.destination` 에 연결하면 **2번 재생되거나 echo** 발생
- 따라서 분기만 한다: `source → analyser` 까지만, downstream 없음

### Chromium 전용 — Safari 미지원
- `HTMLMediaElement.captureStream()` 표준은 Chrome/Edge 만 안정. Firefox 는 부분 지원, Safari 미지원.
- Phase 1 은 Chromium 계열만 타겟. Safari/Firefox 는 *procedural fallback* 로 떨어진다 (정적 sine wave 기반, 트랙별 차이 없음).
- 감지: `typeof audioElement.captureStream === 'function'`

### AudioContext 자동재생 정책
- 사용자 제스처(클릭) 없이 `new AudioContext()` 호출은 `suspended` 상태로 시작
- visualizer 페이지 진입은 사용자가 라우트 이동한 것이므로 제스처로 간주
- 그래도 안전하게: `ctx.state === 'suspended'` 이면 `await ctx.resume()` 후 RAF 루프 시작
- 재생 토글 버튼에 `ctx.resume()` 한 번 더 호출 (이중 안전)

### 메모리/생명주기
- `AudioContext` 는 페이지 unmount 시 `ctx.close()` 명시 호출
- analyser/source 노드는 `ctx.close()` 가 정리
- `audioElement` 는 TIDAL playback module 의 singleton — visualizer 가 소유하지 않음, 끊지 않음

---

## 4. UI 구조

### 4.1 화면 레이아웃 (1440×900 기준)

```
┌────────────────────────────────────────────────────────────┐
│ [<] back                                       [⛶] dock 복귀 │ ← topbar 56px
├──────────────┬─────────────────────────────────────────────┤
│              │                                              │
│  Queue rail  │                                              │
│  (template   │     Fullscreen track image                   │
│   sidebar    │     + EQ overlay                             │
│   차용,      │     (70%+ viewport area)                     │
│   280px      │                                              │
│   고정)      │                                              │
│              ├─────────────────────────────────────────────┤
│              │   ▶ play   ⏮ ⏭   ━━━━━━━ 02:14 / 04:32    │
│              │   shuffle/repeat/heart  · vol               │ ← controls 96px
└──────────────┴─────────────────────────────────────────────┘
```

- 좌측 큐: 템플릿 `.sidebar` ([player.css:153-216](/Users/woosungjo/music-space/imapplepie-music-player/player.css#L153-L216)) 그대로 차용. `PlaybackContext.currentPlaylist?.tracks` 바인딩.
- 메인: **이미지 70%+ 점유** — 트랙 커버를 `object-fit: cover` 로 풀스크린, blur(20px) 배경 + 원본 중앙 정렬. spec §15 충족.
- EQ overlay: 템플릿 `Visualizer` ([player-components.jsx:124-183](/Users/woosungjo/music-space/imapplepie-music-player/player-components.jsx#L124-L183)) 차용, 데이터 소스만 `heightsAt()` 함수 → `analyser.getByteFrequencyData()` 로 교체.
- 컨트롤: 템플릿 controls row 차용, `PlaybackContext` 의 `play/pause/next/prev/seek` 그대로 호출.

### 4.2 애니메이션 풀 (spec §8 "랜덤 풀")

Phase 1 의 풀 = 3종:
1. **Bars** — 템플릿 `Visualizer` 그대로. frequencyData → bar heights.
2. **Radial bloom** — 같은 frequencyData 를 ring 형태로. 신규.
3. **Particle drift** — 저주파(bass) bin 평균을 particle spawn rate 로. 신규.

트랙 변경 시 랜덤 선택. `?animation=bars|radial|particle` 쿼리로 강제 가능 (디버그).

### 4.3 큐/플레이리스트 정보 표시 (spec §6, §7)

- 플레이리스트 재생: `currentPlaylist.name` + `tracks[]` 리스트, 현재 트랙 강조
- 단일 트랙 재생: `currentTrack` 정보만 표시 (큐 rail 은 collapse)
- 트랙 메타: title / artist / album / duration / accent palette
- accent palette 는 트랙 이미지에서 추출 (Phase 1: 단순 `Color Thief` lib 또는 1개 px 샘플링)

---

## 5. 라우팅 & 노출 제어

### 5.1 진입 경로
- `/visualizer` (신규 라우트)
- 기존 `PlaybackDock` 에 "확장 보기" 아이콘 — TIDAL 트랙 재생 중일 때만 표시

### 5.2 TIDAL 전용 가드

```tsx
// VisualizerPage 진입 시
if (currentTrack?.playbackPlatformId !== 'tidal') {
  return <Navigate to="/" replace />
}
```

- spec §3 "다른 플랫폼 사용자는 가려져야" → FE 라우트 가드 + dock 의 확장 버튼 조건부 렌더
- backend 인가는 추가 없음 (재생 자체 가드가 이미 TIDAL OAuth scope 으로 작동 중)

### 5.3 기존 dock 과의 관계
- visualizer 페이지를 떠나면 dock 가 다시 화면에 보임 (기본 동작)
- visualizer 페이지에서는 dock 를 hide (한 화면에 두 플레이어 UI 동시 표시 회피)
- **재생 상태는 동일 PlaybackContext** — 페이지 이동 중에도 재생 유지

---

## 6. 모듈 변경 계획

### 6.1 신규 파일

| 파일 | 책임 |
|---|---|
| `apps/web/src/pages/VisualizerPage.tsx` | 라우트 entry, TIDAL gate, 레이아웃 컴포지션 |
| `apps/web/src/components/visualizer/FullscreenCover.tsx` | 70%+ 이미지 + blur 배경 |
| `apps/web/src/components/visualizer/EqOverlay.tsx` | 3종 애니메이션 풀 라우터 |
| `apps/web/src/components/visualizer/animations/BarsVisualizer.tsx` | 템플릿 차용 |
| `apps/web/src/components/visualizer/animations/RadialBloomVisualizer.tsx` | 신규 |
| `apps/web/src/components/visualizer/animations/ParticleDriftVisualizer.tsx` | 신규 |
| `apps/web/src/components/visualizer/QueueRail.tsx` | 템플릿 sidebar 차용 |
| `apps/web/src/components/visualizer/ControlsBar.tsx` | 템플릿 controls 차용 |
| `apps/web/src/hooks/useTidalAudioAnalyser.ts` | captureStream → AnalyserNode 라이프사이클 |
| `apps/web/src/hooks/useDominantColor.ts` | 이미지 → accent 추출 |

### 6.2 기존 파일 수정 (최소 surface)

| 파일 | 변경 |
|---|---|
| [apps/web/src/lib/tidalStreamPlayback.ts](apps/web/src/lib/tidalStreamPlayback.ts) | `export const getTidalAudioElement = (): HTMLAudioElement \| null` 추가만. 기존 로직 0 변경. |
| [apps/web/src/App.tsx](apps/web/src/App.tsx) | `/visualizer` 라우트 등록 + visualizer 경로에서 PlaybackDock hide |
| [apps/web/src/components/music/PlaybackDock.tsx](apps/web/src/components/music/PlaybackDock.tsx) | TIDAL 트랙일 때만 "확장 보기" 버튼 표시 (단순 prop/조건). 다른 동작 0 변경. |

### 6.3 backend 변경
**없음.** Phase 1 은 backend touch 0.

---

## 7. 데이터 흐름

```
[User clicks 확장 보기 in PlaybackDock]
    ↓
navigate('/visualizer')
    ↓
VisualizerPage mounts
    ├─ guard: currentTrack.playbackPlatformId === 'tidal' ?
    ├─ useTidalAudioAnalyser():
    │     ├─ getTidalAudioElement() → audio
    │     ├─ if (audio.captureStream) → stream → AnalyserNode (path B)
    │     └─ else → return { mode: 'fallback', data: procedural }
    ├─ useDominantColor(currentTrack.coverUrl) → accent
    └─ render: FullscreenCover + EqOverlay + QueueRail + ControlsBar

[Frame (60fps)]
    analyser.getByteFrequencyData(data)
    → EqOverlay 가 활성 애니메이션에 전달
    → CSS bar height / canvas particle 갱신
```

---

## 8. 검증 기준

각 항목은 PR merge 전 통과 필수.

### 8.1 기능
- [ ] `/visualizer` 라우트 진입 시 TIDAL 트랙이면 visualizer 렌더, 아니면 `/` 로 리다이렉트
- [ ] 재생 중 EQ bar 가 음악에 맞춰 움직임 (정적 패턴 아님 — 트랙 별 시각적 차이 확인)
- [ ] pause 시 EQ 가 정지 또는 idle 모드로 전환
- [ ] 트랙 변경 시 cover 이미지 + accent 색상 + 애니메이션 종류 갱신
- [ ] 컨트롤 (play/pause/next/prev/seek/volume) 가 dock 와 동일하게 동작
- [ ] 페이지 떠나면 dock 복귀, 재생 중단 없음

### 8.2 회귀 (rollback 의 교훈)
- [ ] TIDAL 재생 자체가 visualizer 진입 전후로 깨지지 않음 (CORS 회귀 차단)
- [ ] Spotify/Apple/PMS/EMS/GMS 재생 흐름은 visualizer 코드를 거치지 않음
- [ ] visualizer 페이지를 한 번도 열지 않은 사용자에게 visualizer 코드 0 영향
- [ ] `tidalStreamPlayback.ts` 의 기존 export/state 0 변경 (단지 `getTidalAudioElement` 만 추가)

### 8.3 성능
- [ ] FPS ≥ 50 (Chrome devtools Performance, 1440×900, M2 기준)
- [ ] visualizer 페이지 떠난 후 `AudioContext` 가 `closed` 상태 (메모리 leak 차단)
- [ ] RAF 루프가 페이지 hidden (`document.visibilityState`) 일 때 자동 중단

### 8.4 fallback
- [ ] Safari 에서 진입 시 procedural fallback 으로 동작 (재생 깨지지 않음, 단순 sine wave 패턴)
- [ ] `audio.captureStream` 미지원 환경에서 throw 없이 fallback path 진입

---

## 9. 리스크 & 미해결

| ID | 리스크 | 완화 |
|---|---|---|
| V1 | TIDAL CDN 의 segment 응답에서 `Access-Control-Allow-Origin` 미설정이면 captureStream 도 silent zero data 반환 가능성 | Phase 1 초반에 **실측 검증** 우선 — 검증 step 으로 단순 `getByteFrequencyData` 평균값 ≥ 1 확인하는 dev 모드 표시 |
| V2 | `captureStream()` 이 Firefox 에서 partial 지원 — 일부 환경에서 silent zero | feature detect 시 `getByteFrequencyData` 첫 500ms 평균이 0 이면 procedural fallback 으로 자동 전환 |
| V3 | dominant-color 추출 비용 (이미지 로딩 + canvas pixel) | 트랙 변경 시에만 1회, 작은 thumbnail URL (`coverUrl` resize) 사용 |
| V4 | accent palette 가 트랙 이미지 색감과 안 어울리는 경우 (어두운 이미지 → 어두운 accent) | luminance threshold 적용, 너무 어두우면 보정값 + 25% |
| V5 | 라우트 이동 중 `AudioContext` 재생성 비용 | `useTidalAudioAnalyser` 가 페이지 lifetime 으로 1 context, unmount 시 close |

---

## 10. 단계별 PR 계획

본 설계는 1개 PR 로 묶음 (rollback 의 교훈: 작은 surface, 명확한 boundary).

**PR: "Visual EQ Player Phase 1 — TIDAL fullscreen visualizer"**

변경:
- 신규 10 파일 (§6.1)
- 기존 3 파일 최소 수정 (§6.2)
- backend 0
- 새 의존성: 없음 (procedural color extraction, no Color Thief lib)

검증: §8 항목 전체

---

## 11. 명시적 결정 기록

| 결정 | 이유 |
|---|---|
| captureStream (B) 채택 | crossOrigin 설정 회피, TIDAL 재생 보존 |
| destination 에 analyser 연결 ❌ | echo/중복 재생 차단 |
| Safari 미지원, procedural fallback | Phase 1 범위 축소, Phase 2 에서 확장 |
| LLM 텍스트/이미지 deferred | 의존성 큼, 별도 PR |
| 매거진 뉴스 deferred | 데이터 소스 (EMS RSS) 재활용 결정만 기록, 구현 Phase 2 |
| 디폴트 dock 대체 ❌ | rollback 안전성, 사용자 선택형은 Phase 2 |
| backend 변경 0 | 재생 경로 동일, 추가 권한 없음 |
| 1 PR 묶음 | surface 작고 boundary 명확 |

---

## 12. spec → 설계 매핑

| spec 항목 | 설계 위치 |
|---|---|
| §3 TIDAL 전용 노출 | §5.2 라우트 가드 |
| §6 플레이리스트 큐 좌측 표시 | §4.1 Queue rail |
| §7 단일 트랙 재생 시 트랙 정보 | §4.3 fallback 표시 |
| §8 이미지+텍스트 애니메이션, 랜덤 풀 | §4.2 애니메이션 풀 3종 |
| §9 LLM 텍스트/이미지 | Phase 2 (§1) |
| §11 상세 컨트롤 | §4.1 ControlsBar |
| §12 디폴트 플레이어 대체 | §5.3 ❌ Phase 1 — Phase 2 검토 |
| §13 imapplepie 템플릿 차용 | §4.1, §6.1 (5/10 컴포넌트가 템플릿 차용) |
| §14 이미지로 화면 채움 | §4.1 FullscreenCover |
| §15 화면 70%+ | §4.1 layout (좌측 280px = 약 19% @1440px, 나머지 81% 중 controls 96px ≈ 89% 가 이미지 영역) |
| §16 매거진 뉴스 아이디어 | Phase 2 (§1) — EMS RSS 재활용 결정 기록 |
