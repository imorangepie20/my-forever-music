# Visual EQ Music Player — 설계 문서 (v2)

작성: 2026-05-16 (v2 재작성, v1 captureStream 접근 폐기)
연관:
- 제품 스펙: [docs/product/VISUAL_EQ_MUSIC_PLAYER.md](../product/VISUAL_EQ_MUSIC_PLAYER.md)
- 디자인 베이스: [/Users/woosungjo/music-space/imapplepie-music-player/](/Users/woosungjo/music-space/imapplepie-music-player/)

---

## 0. v1 폐기 이유 (꼭 읽고 시작할 것)

v1 은 `HTMLMediaElement.captureStream()` 으로 audio 신호를 분기한다고 가정했다.

실측 결과:
```
Failed to execute 'captureStream' on 'HTMLMediaElement':
Cannot capture from element with cross-origin data
```

원인: TIDAL CDN 의 HLS segment 응답에 `Access-Control-Allow-Origin` 헤더가 없다. HLS.js 가 fetch 해서 MSE SourceBuffer 에 append 한 데이터는 cross-origin tainted 로 분류되고, `<audio>` element 자체도 tainted 가 된다.

이 제약은 모든 element 경계 API 에 동일 적용:
- `createMediaElementSource(audio)` — 거부 (path A)
- `audio.captureStream()` — 거부 (v1 의 path B)

→ 둘 다 element 를 통과해야 해서 막힌다. v1 의 "B 는 crossOrigin 안 받으니 안전" 은 **틀린 가정**이었다.

교훈: 다음에 새 시각화 시도하기 전에 *segment 응답 헤더가 CORS-clean 인지 먼저 확인* 한다.

---

## 1. 새 접근 (v2 path)

**MSE buffer intercept** — HLS.js 가 SourceBuffer 에 append 하기 *직전의* segment ArrayBuffer 를 가로채서, JS heap 에서 직접 디코드한다.

```
HLS.js fetch(segment URL)         ← cross-origin, tainted bytes
  ↓
HLS.js demux → 우리가 가로챔     ← raw ArrayBuffer, element 경계 없음
  ↓
WebCodecs AudioDecoder.decode    ← raw bytes 는 CORS 검사 안 함
  ↓
AudioData (PCM Float32)
  ↓
ring buffer (최근 ~10s 분량 PCM)
  ↓
RAF 루프: audio.currentTime 으로 인덱싱 → FFT → 시각화
```

핵심: **element 를 거치지 않으므로 CORS taint 가 적용되지 않는다.** ArrayBuffer 는 그냥 JS 메모리고, WebCodecs `AudioDecoder` 와 `OfflineAudioContext.decodeAudioData` 는 element 경계 검사를 하지 않는다.

---

## 2. 디코드 전략 — 두 후보, 구현 중 선정

| | API | 입력 | 장점 | 단점 |
|---|---|---|---|---|
| **D1. WebCodecs AudioDecoder** | `new AudioDecoder({ output, error })`, `decoder.configure({ codec, description })`, `decoder.decode(EncodedAudioChunk)` | 각 AAC frame (encoded chunk) | low-latency, frame 단위 streaming | M4S → AAC frame demux 필요, init segment 의 ESDS 에서 codec config 추출 필요 |
| **D2. decodeAudioData (재구성)** | `AudioContext.decodeAudioData(arrayBuffer)` | 완전한 mp4/m4a 컨테이너 | 단순, 검증된 API | init + media segment 를 매 segment 마다 concat 해서 완전 컨테이너 만들어야 함, 메모리 비효율 |

현재 구현 1순위는 **D2 `decodeAudioData` 재구성**이다. hls.js 1.6.16 에서는 과거 `FRAG_PARSING_DATA` 이벤트가 공개 이벤트로 제공되지 않으므로, `BUFFER_CODECS` 에서 init segment 를 잡고 `BUFFER_APPENDING` 의 media segment 와 합쳐 브라우저 디코더로 검증한다.

D2 가 실제 TIDAL segment 에서 실패하면 D1 WebCodecs + 별도 demux 로 전환한다. 둘 다 막히면 — 그건 또 다른 dead-end 라 거기서 멈춘다.

---

## 3. HLS.js 통합 지점

`tidalStreamPlayback.ts` 의 `hls` 싱글톤에 capture listener 를 추가:

```ts
hls.on(Hls.Events.BUFFER_CODECS, (_evt, data) => {
  visualizerSegmentBus.rememberInitSegment(data.audio?.initSegment)
})

hls.on(Hls.Events.BUFFER_APPENDING, (_evt, data) => {
  if (data.type !== 'audio' && data.type !== 'audiovideo') return
  visualizerSegmentBus.push({
    startTime: data.frag.startPTS ?? data.frag.start,
    endTime: data.frag.endPTS,
    payload: data.data,
  })
})
```

- 신규 모듈 `lib/tidalAudioCapture.ts` 가 `visualizerSegmentBus` 를 소유
- subscribe / unsubscribe 패턴 — VisualizerPage 진입 시 subscribe, 떠나면 unsubscribe
- 미 subscribe 상태에서는 listener 자체가 attach 안 되거나 (lazy) 또는 bus 가 drop 만 함 (메모리 leak 방지)
- TIDAL 재생 흐름 0 변경 — 분기만 추가

---

## 4. 시간 동기화

이게 가장 까다로움.

### 4.1 segment 타이밍
- HLS prefetch 로 segment 는 *재생 위치보다 5~10초 앞서* 도착
- 각 segment 의 PTS (presentation timestamp) 가 정확한 미디어 시간을 알려줌
- `audio.currentTime` 은 현재 재생 위치 (초 단위)

### 4.2 ring buffer
- 디코드된 PCM 을 시간축에 따라 순환 버퍼에 저장
- 크기: **최근 ~10s 분량** (현재 위치 ± 여유) — 그 이상 보관할 이유 없음
- 데이터 구조: `{ pts: number, samples: Float32Array }[]` 또는 더 효율적인 contiguous ring

### 4.3 frame 별 sample 조회
```ts
// RAF 루프 내
const t = audio.currentTime
const samples = ringBuffer.readWindow(t, FFT_WINDOW_SAMPLES)
// samples → FFT → frequencyData → 시각화
```

### 4.4 FFT
- WebAudio `AnalyserNode` 는 audio graph 가 있어야 함 → 이 경로엔 graph 없음
- 직접 FFT 구현 또는 작은 라이브러리 (`fft.js` 등) 사용
- size: 256 (8주파수 binCount=128, 기존 v1 과 동일)

### 4.5 동기화 실패 케이스
- ring buffer 에 `t` 시점 데이터가 없음 (seek 직후, 디코드 지연)
  → 잠시 zero 데이터 반환 (`avg 0`), UI 는 idle 모드로 fall back
- segment 디코드가 RAF 보다 느림 → 데이터 부족 → 동일하게 idle

---

## 5. 메모리 & CPU

- ring buffer 10s × 48kHz × 2ch × Float32 = 약 **4MB**
- 디코드 비용 = segment 1개당 한 번 (보통 4초 분량 → 디코드 시간 수십 ms, 메인 스레드 비점유 if WebCodecs)
- FFT 비용 = frame 당 1회, size 256 → 무시 수준

→ 60fps 충분히 가능. WebCodecs 가 worker 스레드 활용하므로 메인 스레드 부담 작음.

---

## 6. UI 구조 (v1 과 거의 동일)

```
┌──────────────┬─────────────────────────────────────────────┐
│ Queue rail   │  Fullscreen track image + EQ overlay (70%+) │
│ (template)   │                                              │
│              ├─────────────────────────────────────────────┤
│              │   Controls bar                               │
└──────────────┴─────────────────────────────────────────────┘
```

비주얼라이저 풀 3종 (v1 과 동일):
- bars
- radial bloom
- particle drift

차이: 모든 비주얼라이저가 받는 데이터가 **진짜 PCM 기반 FFT**. fallback 모드 자체가 없음 — ring buffer 가 비면 idle (정지 상태 표현).

---

## 7. 라우팅 & 노출

- `/visualizer` 라우트 (v1 과 동일)
- TIDAL 재생 중일 때만 dock 의 확장 버튼 표시
- TIDAL 아니면 진입 시 `/` 로 리다이렉트
- backend 변경 0

---

## 8. 파일 구조

### 신규
| 파일 | 책임 |
|---|---|
| `apps/web/src/lib/tidalAudioCapture.ts` | HLS.js `BUFFER_CODECS`/`BUFFER_APPENDING` listener, segment bus, subscribe API |
| `apps/web/src/lib/audioRingBuffer.ts` | 시간축 ring buffer, readWindow API |
| `apps/web/src/lib/segmentDecoder.ts` | init + media segment 를 `decodeAudioData` 로 PCM 변환 |
| `apps/web/src/lib/simpleFFT.ts` | size-256 FFT (또는 라이브러리 한 번 검토 후 결정) |
| `apps/web/src/hooks/useTidalAudioAnalyser.ts` | ring buffer + FFT 결합, `read(target)` API 제공. 이름은 기존 컴포넌트 연결을 유지하기 위해 보존 |
| `apps/web/src/hooks/useDominantColor.ts` | (재사용 — v1 과 동일) |
| `apps/web/src/pages/VisualizerPage.tsx` | (재사용 — PCM 기반 useTidalAudioAnalyser 연결) |
| `apps/web/src/components/visualizer/*` | (재사용) |

### 기존 수정
| 파일 | 변경 |
|---|---|
| `apps/web/src/lib/tidalStreamPlayback.ts` | HLS 인스턴스 생성/삭제 시 `tidalAudioCapture` attach/detach |
| `apps/web/src/App.tsx` | `/visualizer` 라우트 등록 (이미 있음) |
| `apps/web/src/components/music/PlaybackDock.tsx` | TIDAL 전용 확장 버튼 (이미 있음) |

### 폐기
- `apps/web/src/hooks/useTidalAudioAnalyser.ts` (v1 captureStream 기반 — 삭제)
- `tidalStreamPlayback.ts` 의 `getTidalAudioElement` export (v1 가 사용했던 노출 — 필요 없어짐, 삭제)

---

## 9. 단계별 PR

| PR | 내용 | 검증 |
|---|---|---|
| **PR A** | v1 visualizer 코드 전체 rollback (1 커밋). docs 만 남김 | build/lint green, /visualizer 404 |
| **PR B-0 (실험)** | `tidalAudioCapture` 만 추가하고 console.log 로 segment 도착 확인 — listener 가 실제로 fire 되는지, payload shape 확인 | 콘솔에 segment 들어옴, type/PTS 노출 |
| **PR B-1 (디코더)** | `decodeAudioData(init + media)` 로 segment 1개 디코드 → PCM Float32 로그 출력 | 콘솔에 sample 값 (0 아닌) 출력 |
| **PR B-2 (ring buffer + 동기화)** | ring buffer + `readWindow(t)` → 매 frame 호출 → 평균값 표시 | `audio.currentTime` 따라 평균값 변동 |
| **PR B-3 (FFT + UI 통합)** | FFT + 비주얼라이저 풀 3종 다시 연결 + Page 통합 | spec 8.1 / 8.2 검증 |

**PR B-0 에서 막히면 거기서 멈춘다** — HLS.js 가 segment payload 를 기대대로 노출 안 하면 D2 (decodeAudioData + 컨테이너 재구성) 로 전환하거나 옵션 자체를 폐기. v1 의 실수를 반복하지 않기 위해 **실험 단계가 PR 로 분리되어 있다**.

---

## 10. 명시적 결정

| 결정 | 이유 |
|---|---|
| MSE intercept (path B v2) 채택 | captureStream/createMediaElementSource 둘 다 CORS taint 로 차단 — element 경계 우회만이 유일한 client-side 경로 |
| WebCodecs AudioDecoder 1순위 | Chrome 94+ 안정, frame 단위 streaming, worker 활용 |
| 서버 프록시 채택 ❌ | 대역폭 2배 + TIDAL TOS 위반 위험 |
| procedural fallback 제거 | spec "Visual EQ" 정체성과 충돌. ring buffer 비면 idle 표현 |
| 실험 단계 PR 분리 | v1 의 1-PR-then-rollback 패턴 차단 |
| Safari/Firefox 지원 | Phase 1 외 — WebCodecs 호환성 확인 후 결정 |

---

## 11. 리스크 (v1 보다 큼, 솔직히)

| ID | 리스크 | 대응 |
|---|---|---|
| R1 | hls.js 1.6.16 에서 `FRAG_PARSING_DATA` 가 공개 이벤트가 아님 | `BUFFER_CODECS`/`BUFFER_APPENDING` 기반으로 변경 완료 |
| R2 | `decodeAudioData(init + media)` 가 TIDAL fMP4 fragment 를 거부함 | PR B-1 에서 확인. 막히면 WebCodecs + demux 로 전환 |
| R3 | segment PTS 와 `audio.currentTime` 의 0점이 다름 (offset) | ring buffer 의 첫 segment PTS 를 zero offset 으로 잡고 currentTime 도 동일 기준 |
| R4 | seek 직후 ring buffer 에 데이터 없음 | idle 표현으로 fall back (procedural 아닌 정지된 모습) |
| R5 | 디코드 latency 가 RAF 보다 느려서 표시 데이터가 항상 ~50ms 지연 | 사람 눈에 50ms 시각화 지연은 인지 거의 안 됨. 무시 가능 |

---

## 12. spec 충족 (변동 없음 — v1 과 동일 매핑)

§3 TIDAL 전용, §6 큐, §7 트랙 정보, §8 애니메이션 풀, §11 컨트롤, §13 템플릿, §14/§15 이미지 70%+ 는 v1 과 동일하게 충족. **§ "Visual EQ" 정체성은 이번에 v2 path 로 진짜로 충족** — v1 은 가짜로 충족하던 셈.

§9 LLM 텍스트/이미지, §16 매거진 뉴스는 여전히 Phase 2.
