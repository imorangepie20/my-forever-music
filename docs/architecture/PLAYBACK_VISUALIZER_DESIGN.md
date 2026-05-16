# Playback Visualizer Design

작성일: `2026-05-15`

이 문서는 `/playback/visualizer` 페이지에서 동작하는 bar-style audio visualizer의 **기술 설계 계약**입니다. 이전 Phase A/B/C 구현은 외부 기술 제약을 고려하지 않고 진입한 결과 PMS/EMS 무음, TIDAL 403, "captureStream tainted" 경고 등 여러 회귀를 만들었습니다. 이 문서는 그 회귀를 다시 만들지 않도록 **무엇이 가능하고 무엇이 금지인지**를 외부 표준/공급자 정책에 근거해 못박는 것이 목적입니다.

이전 코드를 정리하고 다시 시작하는 것을 전제로 합니다.

---

## 1. Scope

### 1.1 이 문서가 다루는 것
- `/playback/visualizer` 페이지의 데이터 소스 선택 근거
- 메인 재생 경로(Spotify Web Playback SDK, TIDAL `<audio>` + HLS.js)에 visualizer가 영향을 **주지 않게** 하는 규칙
- 명시적으로 사용 금지된 Web API 호출과 그 이유
- 1차/2차 구현 단계와 각 단계의 검증 기준

### 1.2 이 문서가 다루지 않는 것
- 시각 디자인(색, 모션 곡선, 막대 수): UI 계층 자유
- `/playback/visualizer` 외 페이지의 mini-equalizer: 같은 규칙을 따르되 별도 결정
- TIDAL OAuth `streaming` scope 누락(§9 참고). 이건 visualizer와 무관한 별개 트랙

---

## 2. 기술 제약 (외부 표준/정책 근거)

이 절은 1차 결정의 **근거 자료**입니다. 의사결정과 결합되므로 발췌 인용을 함께 둡니다.

### 2.1 `createMediaElementSource()`는 출력 라우팅을 영구히 옮긴다

[MDN — `AudioContext.createMediaElementSource()`](https://developer.mozilla.org/en-US/docs/Web/API/AudioContext/createMediaElementSource) 원문:

> "As a consequence of calling `createMediaElementSource()`, audio playback from the `HTMLMediaElement` will be re-routed into the processing graph of the AudioContext."

즉 한 번 호출하면 그 `<audio>` 요소의 출력은 element 내부 sink → AudioContext graph로 **영구히 이동**합니다. 표준 어디에도 이 라우팅을 되돌리는 방법이 없습니다. `source.disconnect()`는 graph 안의 연결만 끊고, 원래의 element-native sink로 음향을 되돌리지 못합니다.

따라서 graph가 destination까지 닿지 않거나(누락된 `connect(ctx.destination)`), AudioContext가 `suspended` 상태이거나, 페이지 이동 등으로 graph가 dispose되면 음향은 **사라집니다**. 다른 페이지에서 같은 element를 재사용해도 마찬가지로 무음입니다. — Phase C 첫 시도가 PMS/EMS 무음을 만든 정확한 메커니즘.

### 2.2 AudioContext autoplay policy: 항상 suspended로 시작한다

[MDN — Web Audio API Best Practices](https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API/Best_practices), [Chrome for Developers — Autoplay policy in Chrome](https://developer.chrome.com/blog/autoplay):

> "If an `AudioContext` is created before the document receives a user gesture, it will be created in the 'suspended' state."

페이지 로드 직후 또는 라우팅된 새 페이지에 진입한 직후의 AudioContext는 거의 항상 `suspended`입니다. `resume()`은 신뢰 가능한 user gesture(주로 `click`) 콜백 안에서 호출돼야 하고, 그 외 시점의 호출은 promise reject 됩니다.

→ visualizer가 element-source attachment에 의존하면 페이지 진입 시점 ↔ 사용자 클릭 시점 사이에 무음 구간이 발생합니다. 본 재생 경로는 이 제약을 받으면 안 됩니다.

### 2.3 `captureStream()`와 cross-origin: 침묵 스트림 가능성

[MDN — `HTMLMediaElement.captureStream()`](https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement/captureStream)와 [WebAudio/web-audio-api issue #2547](https://github.com/WebAudio/web-audio-api/issues/2547) 요약:

- `captureStream()`은 `createMediaElementSource()`와 달리 **재생 라우팅을 옮기지 않는다**. 그 점에서는 안전.
- 그러나 cross-origin 자원에 대해서는 다음 조건을 둘 다 충족해야 한다:
  1. 응답에 `Access-Control-Allow-Origin` 헤더 존재
  2. `HTMLMediaElement`에 `crossorigin` 속성 설정
- 둘 중 하나라도 빠지면 stream은 동작하되 **데이터가 zero/black**으로 흘러간다(silently tainted). 우리가 보던 "FFT 0 데이터 전달" 경고가 정확히 이 경로.
- 추가로 EME(DRM) 콘텐츠에서는 capture 자체가 차단된다(`Stream capture not supported with EME`).

TIDAL은 HLS.js로 MSE 기반 `<audio>`에 segment를 채워 넣는 구조([HLS.js GitHub](https://github.com/video-dev/hls.js/)). 세그먼트 CDN의 CORS posture와 `<audio>` 요소의 `crossorigin` 속성을 모두 통제할 수 있어야 비로소 의미 있는 FFT가 나옵니다. TIDAL Manifest API 응답 헤더는 우리가 직접 강제할 수 없는 영역.

### 2.4 Spotify `audio-analysis`/`audio-features` 신규 앱 제한

[Spotify for Developers — Introducing some changes to our Web API (2024-11-27)](https://developer.spotify.com/blog/2024-11-27-changes-to-the-web-api):

신규 application은 `Audio Features`, `Audio Analysis`, `Recommendations`, `Related Artists`, `Featured/Category Playlists`, multi-get 응답의 `30s preview URLs`, algorithmic/editorial playlists 모두 사용 불가. 대안 endpoint **없음**. 기존 extended-quota 앱만 유지.

→ Phase B에서 만든 [`useSpotifyAudioAnalysisAdapter`](../apps/web/src/components/visualizer/useSpotifyAudioAnalysisAdapter.ts)는 신규 앱 등록 이후 401/403 받습니다. 본 서비스의 audio feature 전략은 이미 [AUDIO_FEATURE_PROVIDER_STRATEGY.md](AUDIO_FEATURE_PROVIDER_STRATEGY.md)에서 ReccoBeats로 옮기기로 결정됨. visualizer도 같은 결정을 따른다.

### 2.5 Spotify Web Playback SDK: element 접근 불가

Web Playback SDK는 내부의 EME(Encrypted Media Extensions) 보호된 `<audio>` 또는 Widevine-protected MediaSource를 사용. SDK는 외부에 element handle을 제공하지 않고, 설사 잡아낸다 해도 §2.3에 따라 EME capture는 차단됩니다. **Spotify 재생 element의 FFT는 표준상 불가**.

### 2.6 HLS.js + MSE + Web Audio 호환성

HLS.js는 MSE 위에 fMP4 fragment를 채우는 방식([HLS.js GitHub](https://github.com/video-dev/hls.js/), [W3C Media Source Extensions](https://www.w3.org/TR/media-source-2/)). Web Audio attachment 자체는 가능하지만 §2.3의 CORS 게이트가 동일하게 적용. 또한 `<audio>`가 MSE source로부터 segment를 받는 동안 `createMediaElementSource`를 호출하면 buffer underrun 시점에 audio sink 재초기화 이슈가 다수 보고됨([Bugzilla 1178751](https://bugzilla.mozilla.org/show_bug.cgi?id=1178751)).

### 2.7 현재 TIDAL 재생 구현 (코드 기준)

본 절은 [`apps/web/src/lib/tidalStreamPlayback.ts`](../apps/web/src/lib/tidalStreamPlayback.ts) 현재 코드 상태를 기준으로 한 사실 기술이다. 향후 코드가 바뀌면 본 절도 갱신한다.

**Element 생성과 부착 상태**:

[`ensureAudioElement` (L149-159)](../apps/web/src/lib/tidalStreamPlayback.ts#L149-L159) 가 `document.createElement('audio')`로 element를 생성하고 `preload = 'auto'`, `volume = 0.5`만 설정한다. **element는 DOM에 append되지 않는 detached element**다. 또한 [모듈 주석 L33-39](../apps/web/src/lib/tidalStreamPlayback.ts#L33-L39)에 다음 의도가 명시되어 있다:

> "crossOrigin 속성을 변경하면 TIDAL CDN 의 CORS 설정에 따라 재생이 깨질 수 있으므로 이 모듈에서는 그대로 두고, Visualizer 쪽이 AnalyserNode 가 0 데이터를 받는 상황을 surface 한다."

즉 `crossorigin` 속성은 의도적으로 미설정. §2.3 규칙상 이 element에 `createMediaElementSource()` 또는 `captureStream()`을 부착하면 cross-origin segment(TIDAL CDN)로부터 들어오는 audio는 **silent zero**로 처리된다. 우리가 이전 phase에서 본 "FFT 0 데이터" 경고가 정확히 이 경로.

**스트림 부착 경로**:

[`playHlsStream` (L225-254)](../apps/web/src/lib/tidalStreamPlayback.ts#L225-L254) 가 HLS.js (`enableWorker: true`)로 `attachMedia(audio)` 호출. Safari 같이 native HLS를 지원하는 경우 `audio.src = streamUrl` 직접 할당. DASH는 [L305-307](../apps/web/src/lib/tidalStreamPlayback.ts#L305-L307) 에서 throw — 지원하지 않음. asset_presentation 이 `FULL`이 아니면 [L296-298](../apps/web/src/lib/tidalStreamPlayback.ts#L296-L298) 에서 throw([`PLAYBACK_ERROR_HANDLING_POLICY.md`](PLAYBACK_ERROR_HANDLING_POLICY.md)의 "TIDAL Playback Rule" 준수).

**모듈 singleton 와 reset 의미**:

`audioElement`, `hls`, `currentProductId` 등 모두 module-level singleton. [`tidalReset()` (L334-340)](../apps/web/src/lib/tidalStreamPlayback.ts#L334-L340)는 `resetSource()` 호출로 hls 인스턴스를 destroy하고 element의 src만 제거할 뿐, **element 자체는 살아 있는다**. 즉 한 번 `createMediaElementSource()`가 그 element에 부착되면 (§2.1) tidalReset 이후의 모든 후속 재생도 그 graph를 거치게 된다.

**Visualizer 관점의 결론**:

본 element에 visualizer가 직접 attach하려면 다음 중 하나가 필요한데, 셋 다 가용하지 않다:
- (a) `crossorigin="anonymous"` 설정 + TIDAL CDN의 `Access-Control-Allow-Origin` 응답 — 우리가 CDN 헤더를 통제할 수 없고, 위 모듈 주석은 변경 시 재생 회귀를 경고
- (b) TIDAL 측이 ACAO와 CORS preflight를 우리 origin에 열어 주는 공식 옵션 — 현재 streaming scope 자체도 미해결(§9), 헤더 협의는 더 멀다
- (c) HLS.js의 `fLoader` hook으로 segment를 우리가 직접 fetch → 우리 Worker에서 디코드 → 별도 Web Audio graph에 흘리기 — 가능은 하나 본 재생 buffer와 디코더 동기가 깨지면 무음 회귀. 옵션 C 카테고리에 해당. **거부**.

따라서 TIDAL은 옵션 A 메타데이터 procedural로만 다룬다. 향후 옵션 B harness(§3.2)는 TIDAL CDN이 아닌 자체 호스팅 sample 으로만 검증.

---

## 3. 데이터 소스 옵션 매트릭스

| 옵션 | 신호원 | 주재생 영향 | 외부 의존 | 결론 |
|------|--------|------------|-----------|------|
| **A. 메타데이터 procedural** | `currentItem.sourcePlatform`, BPM/energy/valence (있으면 사용, 없으면 기본 preset), `isPlaying`, 시간축 | 없음 | 없음 | **1차 채택** |
| **B. 별도 audio element + Web Audio** | preview URL 또는 별도 디코드된 sample을 visualizer 전용 `<audio>`에 로드, 그 element에만 `captureStream()` 또는 `createMediaElementSource()` 부착 | 없음(별도 element이므로) | preview URL 가용성, CORS 호환 | **격리 harness 후 평가** |
| **C. 메인 재생 element에 Web Audio 부착** | Spotify SDK element / TIDAL `<audio>`에 직접 attach | **있음** (§2.1, §2.5) + CORS 게이트(§2.3) | 큼 | **거부** |

### 3.1 옵션 A 상세

신호 함수 예 (의사 코드):

```
heightsAt({ count, t, mode, beatStrength }) =>
  for i in 0..count:
    base = preset(mode)[i]                    // 막대별 기본 진폭 곡선
    pulse = beatEnvelope(t, bpm)              // 0..1, BPM 기반 ADSR
    energy = audioFeatures?.energy ?? 0.6     // ReccoBeats 보강 결과
    out[i] = clamp(base * (0.4 + 0.6*pulse) * energy, 0, 1)
```

특징:
- **외부 호출 0**. ReccoBeats가 channel별 audio feature를 채우는 시점에 자동으로 좋아짐, 없으면 합리적 default.
- BPM이 있으면 박자 envelope이 살아 움직임. 없으면 mode별 정적 패턴.
- 진폭은 입력 FFT가 아니라 envelope이므로 항상 시각적으로 그럴듯하지만 **소리에 정확히 동기되지 않는다**. 이 한계는 UX 문구로 명시(§5.3).

### 3.2 옵션 B 상세 (Phase 2 후속)

이건 [`REAL_IMPLEMENTATION_POLICY.md`](REAL_IMPLEMENTATION_POLICY.md) §4의 "복잡한 통합은 격리 페이지를 먼저"에 정면으로 부합하는 형태로 갑니다:

- `/dev/visualizer-harness` (개발용 전용 페이지) 만들기
- 그 페이지 안에 visualizer 전용 `<audio crossorigin="anonymous">` 요소를 별도로 생성
- 이 요소에 알려진-호환 mp3 (예: 자체 호스팅된 30초 sample, CORS 헤더 통제 가능)를 로드
- 메인 재생과는 **완전 분리**된 AudioContext에서만 FFT 수집
- 측정 결과: 정상 FFT가 나오는지, autoplay/visibility/route 변경에 안전한지 확인

이 harness가 4개 이상의 환경(Chrome desktop, Safari desktop, Chrome Android, iOS Safari)에서 안정 시연되기 전까지 **메인 visualizer 페이지에 들이지 않는다**.

### 3.3 옵션 C가 거부되는 정확한 이유

이전 시도에서 발견된 회귀를 외부 근거 + 현재 TIDAL 구현(§2.7)과 매칭:

1. PMS/EMS 무음 ← §2.1 (영구 라우팅 + graph 끊김). [`useTidalAnalyserAdapter`](../apps/web/src/components/visualizer/useTidalAnalyserAdapter.ts) 가 처음에 `createMediaElementSource(audioElement)`를 호출했고, [`tidalStreamPlayback.ts`](../apps/web/src/lib/tidalStreamPlayback.ts) 의 module-level singleton element가 영구히 graph 안으로 들어갔다. 이후 다른 사용자 흐름(PMS/EMS)도 같은 module을 재사용하면서 무음이 누적됨.
2. "FFT 0 데이터" 경고 ← §2.3 + §2.7 (CORS-tainted silent stream). `crossorigin` 속성을 의도적으로 안 붙이는 코드 결정과 정면 충돌. `captureStream()`으로 우회해도 같은 게이트가 적용되어 결과는 silent zero.
3. TIDAL 재생 토큰 만료 시 graph dispose → 무음 ← §2.1 + §2.2
4. Spotify SDK element 자체에 접근 불가 ← §2.5

옵션 C는 위 4가지 중 어느 하나라도 발생 시 본 재생이 망가집니다. 본 재생은 절대 visualizer 때문에 망가져서는 안 됩니다(§4).

---

## 4. 1차 구현 결정

### 4.1 Phase 1 — 옵션 A baseline

- `/playback/visualizer` 페이지는 옵션 A만으로 동작한다.
- 데이터: `currentItem.sourcePlatform`, `isPlaying`, `positionMs`, 그리고 [`AUDIO_FEATURE_PROVIDER_STRATEGY.md`](AUDIO_FEATURE_PROVIDER_STRATEGY.md) 가 채우는 `audio_feature` (BPM/energy/valence)를 PMS에서 조회.
- Web Audio API 호출 **0회**. `AudioContext` 생성 자체를 하지 않는다.
- 페이지 컴포넌트 구조:
  - `VisualizerPage.tsx` (라우트, 컨트롤, 상단 메타)
  - `Visualizer.tsx` (순수 시각 표현, `heightsAt(sample)` prop 또는 내장 default)
  - `proceduralEnvelope.ts` (BPM/energy/mode → bar heights 함수, 순수 함수)

### 4.2 Phase 2 — 옵션 B harness (별도 PR, 본 도입 아님)

- `/dev/visualizer-harness` 전용 페이지에 격리.
- 환경별 호환성 시연 영상 또는 자동화 로그가 본 문서에 부록으로 추가될 때까지 prod에 흘리지 않는다.
- 본 문서 §3.2의 통과 기준 4개 환경.

### 4.3 Phase 3 — 옵션 B의 prod 도입 (조건부)

Phase 2 harness가 합격하면 feature flag (`playback.visualizer.realFft`) 뒤에 단계적 도입. 메인 재생 element가 아니라 **visualizer 전용 보조 element**에만 attach한다는 본 문서의 원칙은 prod 도입 시에도 유지.

---

## 5. UX와 실패 경계

### 5.1 사용자 흐름

- 진입: PlaybackDock의 maximize 또는 queue chip
- 페이지 내: 메인 transport(재생/일시정지/다음/이전), 트랙 메타, bar visualizer, 진행 바
- 종료: 뒤로가기 또는 라우트 이탈. PlaybackContext 그대로 유지, 재생 비차단.

### 5.2 실패 시나리오와 처리

| 실패 | 옵션 A 동작 | 사용자 표시 |
|------|------------|-------------|
| `currentItem` 없음 | "재생 중인 트랙이 없습니다" placeholder | 기존 텍스트 유지 |
| BPM/energy 미보강 | mode별 default preset 사용 | 별도 경고 없음(정상 fallback) |
| `audio_feature.unavailable` | default preset | 작게 "신호: mode preset" 라벨 |
| 라우트 이탈 후 재진입 | 재계산 시작 | 영향 없음 |

### 5.3 정직성 라벨

visualizer 하단에 현재 신호 종류를 작게 표시:

- "신호: BPM/energy envelope" (audio_feature 보강 완료)
- "신호: mode preset" (보강 미완)

옵션 A는 본질적으로 음향에 정확히 동기되지 않으므로 "실시간 분석"으로 호도하지 않는다. 옵션 B/C가 prod에 들어오기 전까지 "real-time FFT" 같은 라벨 금지.

---

## 6. 명시적 비스코프 (금지 API/패턴)

다음은 본 visualizer 코드 어디에서도 **사용 금지**:

1. **`new AudioContext()` / `new (window.webkitAudioContext)()`** — Phase 1 동안 금지. Phase 2/3은 §3.2 harness 안에서만 허용.
2. **`audioCtx.createMediaElementSource(mediaEl)`** — 메인 재생 element에 영구 적용. 어떤 phase에서도 금지.
3. **메인 재생 element에 `captureStream()`** — Phase 1 금지. Phase 2/3에서도 메인 재생 element에는 금지. 별도 visualizer-전용 element에만 허용.
4. **Spotify `audio-analysis` / `audio-features` endpoint 직접 호출** — visualizer 코드 경로에서 금지. PMS audio_feature 컬럼 조회만 허용.
5. **임의 fake 수치 생성** — BPM/energy가 없을 때 mode preset으로 떨어지되, "estimated BPM = 120"식 fabricated 값을 audio_feature 슬롯에 흘리지 않는다. [`REAL_IMPLEMENTATION_POLICY.md`](REAL_IMPLEMENTATION_POLICY.md)의 가짜 데이터 금지 원칙 그대로.
6. **다른 페이지의 PlaybackContext 동작 변경** — visualizer는 read-only. `pause/resume/skip`는 기존 컨트롤을 호출만 한다.

이 목록 어긋남은 PR 리뷰에서 즉시 거절. 새로운 시도가 정말 필요하면 §3.2 harness로 가라.

---

## 7. 회귀 방어

다음 회귀를 다시 발생시키지 않기 위한 구체적 검증:

1. **PMS/EMS 무음 회귀**: visualizer 페이지 진입 → 나가기 후 PMS/EMS 트랙 재생 → 정상 출력. 매 PR의 수동 smoke test 필수.
2. **TIDAL 재생 영향 0**: visualizer 페이지를 한 번도 안 열고 / 열고 / 열었다 닫고 세 케이스에서 TIDAL 재생이 동일하게 동작.
3. **Spotify 재생 영향 0**: 동일 케이스 Spotify에 대해 적용.
4. **AudioContext suspended 회귀 0**: Phase 1 동안 AudioContext가 코드 어디에서도 생성되지 않는지 grep으로 CI 차단(`AudioContext|webkitAudioContext` 검색이 visualizer 디렉토리에 0건이어야 함).
5. **CORS-tainted 경고 0**: Phase 1 동안 발생 자체가 불가능(Web Audio 미사용). Phase 2 harness에서만 측정.

§7.4의 grep 차단은 CI에서 lint rule 또는 husky pre-push로 구현. PR 1에 포함.

---

## 8. 기존 코드 정리 (선행 작업)

본 문서 채택 시 다음 파일을 **삭제 또는 재작성**:

| 파일 | 처리 |
|------|------|
| [`apps/web/src/components/visualizer/useTidalAnalyserAdapter.ts`](../apps/web/src/components/visualizer/useTidalAnalyserAdapter.ts) | 삭제 |
| [`apps/web/src/components/visualizer/useSpotifyAudioAnalysisAdapter.ts`](../apps/web/src/components/visualizer/useSpotifyAudioAnalysisAdapter.ts) | 삭제 |
| [`apps/web/src/components/visualizer/spotifyAudioAnalysis.ts`](../apps/web/src/components/visualizer/spotifyAudioAnalysis.ts) | 삭제 |
| [`apps/web/src/lib/tidalStreamPlayback.ts`](../apps/web/src/lib/tidalStreamPlayback.ts) — `getTidalAudioElement` export 제거 | 외부 모듈에 element를 노출하면 §6.2(메인 element attach 금지)를 유도. 모듈 주석 L33-39 본문은 유지(§2.7 근거 문서) |
| [`apps/web/src/pages/VisualizerPage.tsx`](../apps/web/src/pages/VisualizerPage.tsx) | 재작성 (옵션 A만) |
| [`apps/web/src/components/visualizer/Visualizer.tsx`](../apps/web/src/components/visualizer/Visualizer.tsx) | 유지하되 `heightsAt` 인자형만 §4.1과 정합 확인 |
| `proceduralEnvelope.ts` | 신규 추가 (옵션 A 신호함수) |

삭제 PR과 도입 PR을 분리한다. 도입 PR에서 §7의 회귀 방어 5건 모두 통과 확인.

---

## 9. 별개 트랙 — TIDAL 403 streaming scope

본 문서 범위 밖이지만 기록:

- 증상: TIDAL playbackinfo 호출 시 `403 access denied`, `scopes=[w_usr, w_sub, r_usr]`, `legacy_streaming_scopes=true`.
- 원인 가설: OAuth scope에 `streaming`이 빠짐. legacy fallback도 거부됨.
- [`PLAYBACK_ERROR_HANDLING_POLICY.md`](PLAYBACK_ERROR_HANDLING_POLICY.md)의 "TIDAL Playback Rule"에 따라 silent fallback 금지. 별도 backend OAuth scope 확인 PR로 처리.

이 이슈는 visualizer 도입/롤백 어느 쪽과도 무관. visualizer PR에서 손대지 않는다.

---

## 10. 의사결정 요약

- 메인 재생 경로에는 Web Audio API 호출을 도입하지 않는다(§2.1, §2.3, §2.5).
- 1차는 옵션 A — 메타데이터 procedural — 으로만 출발한다(§4.1).
- 실제 FFT가 정말 필요하다면 격리 harness에서 검증한 뒤에만 prod에 들인다(§3.2, §4.2/4.3).
- Spotify audio-analysis는 사용하지 않는다. ReccoBeats 보강 결과를 PMS에서 조회한다(§2.4).
- TIDAL 403은 본 트랙과 분리한다(§9).

## 11. 참고 자료

- MDN — [`AudioContext.createMediaElementSource()`](https://developer.mozilla.org/en-US/docs/Web/API/AudioContext/createMediaElementSource)
- MDN — [`MediaElementAudioSourceNode`](https://developer.mozilla.org/en-US/docs/Web/API/MediaElementAudioSourceNode)
- MDN — [`HTMLMediaElement.captureStream()`](https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement/captureStream)
- MDN — [Web Audio API Best Practices](https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API/Best_practices)
- MDN — [Autoplay guide for media and Web Audio APIs](https://developer.mozilla.org/en-US/docs/Web/Media/Guides/Autoplay)
- MDN — [Media Source Extensions API](https://developer.mozilla.org/en-US/docs/Web/API/Media_Source_Extensions_API)
- Chrome for Developers — [Autoplay policy in Chrome](https://developer.chrome.com/blog/autoplay)
- Chrome for Developers — [Web Audio, Autoplay Policy and Games](https://developer.chrome.com/blog/web-audio-autoplay)
- W3C — [Media Source Extensions™](https://www.w3.org/TR/media-source-2/)
- WebAudio/web-audio-api — [Issue #2547: createMediaElementSource() and captureStream() on cross-origin resources](https://github.com/WebAudio/web-audio-api/issues/2547)
- Mozilla Bugzilla — [#1178751: mozCaptureStream on HTMLMediaElement should not destroy the AudioSink](https://bugzilla.mozilla.org/show_bug.cgi?id=1178751)
- HLS.js — [github.com/video-dev/hls.js](https://github.com/video-dev/hls.js/)
- Spotify for Developers — [Introducing some changes to our Web API (2024-11-27)](https://developer.spotify.com/blog/2024-11-27-changes-to-the-web-api)
- 내부 — [`AUDIO_FEATURE_PROVIDER_STRATEGY.md`](AUDIO_FEATURE_PROVIDER_STRATEGY.md)
- 내부 — [`REAL_IMPLEMENTATION_POLICY.md`](REAL_IMPLEMENTATION_POLICY.md)
- 내부 — [`PLAYBACK_ERROR_HANDLING_POLICY.md`](PLAYBACK_ERROR_HANDLING_POLICY.md)
