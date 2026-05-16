# Visual EQ Actual Runbook

이 runbook 은 TIDAL Visual EQ 의 새 신호 경로를 실제 브라우저에서 확인하기 위한 절차다.

## 목적

- 기존 `captureStream()` 경로가 아니라 HLS.js `BUFFER_CODECS`/`BUFFER_APPENDING` 에서 잡은 bytes 로 PCM 을 생성하는지 확인한다.
- `/visualizer` 화면의 `avg` / `peak` 값이 실제 decoded PCM 기반으로 변하는지 확인한다.
- 실패 시 procedural animation 으로 숨기지 않고 `error`/`waiting` 이유를 확인한다.

## 실행

1. API 서버를 재시작한다. direct MP4 분석 fallback 은 `services/api` 의 `/analysis-audio` endpoint 를 사용하므로 서버 재시작 전에는 적용되지 않는다.

2. 웹 앱을 실행한다.

```bash
cd /Users/woosungjo/music-space/my-forever-music/apps/web
npm run dev -- --host 127.0.0.1
```

3. 브라우저에서 접속한다.

```text
http://localhost:5173
```

4. TIDAL 계정이 연결된 사용자로 로그인하고 TIDAL 트랙 또는 TIDAL 로 resolve 가능한 트랙을 재생한다.

5. 플레이어 dock 의 확장 버튼을 눌러 `/visualizer` 로 이동한다.

6. 화면 우상단 진단 값을 확인한다.

```text
TIDAL · Visual EQ · pcm
avg <0보다 큰 값> · peak <0보다 큰 값> · decoded <seconds>s segment
```

## Console Probe

DevTools Console 에서 아래 순서로 확인한다.

```js
window.__visualizerProbe.reset()
window.__visualizerProbe.start()
```

TIDAL 재생을 시작한 뒤:

```js
window.__visualizerProbe.status()
await window.__visualizerProbe.decodeLatest()
```

성공 기준:

- `status().codecs` 에 `audio` 또는 `audiovideo` init segment 가 있다.
- `status().recentSegments` 에 `kind: "media"` segment 가 있다.
- `decodeLatest()` 결과가 `{ ok: true }` 이고 `firstSamples` 가 전부 0 이 아니다.

실패 기준:

- `no init segment captured`: HLS init segment capture 경계 문제.
- `decodeAudioData FAILED`: init + media 재구성이 Chrome 디코더에 충분하지 않음. 다음 단계는 WebCodecs + demux 경로다.
- `/visualizer` 가 계속 `waiting`: HLS stream 이 아니거나 TIDAL playback 이 아직 시작되지 않음.
- `direct TIDAL stream detected`: TIDAL 이 HLS manifest 가 아닌 direct audio URL 을 반환했다. 브라우저 fetch/decode 를 시도한 뒤 `pcm` 또는 `error` 로 바뀌어야 한다.
- `browser direct fetch failed (...); API analysis fetch failed (...)`: 브라우저가 CDN URL 을 직접 읽지 못했고, 같은 origin API fallback 도 실패했다. API 서버 재시작 여부와 `/api/v1/platforms/playback/tidal/tracks/{id}/analysis-audio` 응답을 확인한다.
- `native HLS playback does not expose HLS.js segments`: Safari/native HLS 경로라 HLS.js intercept 를 사용할 수 없다.
