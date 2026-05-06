# ReccoBeats Audio Feature Reference

> **최종 업데이트:** 2026-05-06
> **공식 사이트:** https://reccobeats.com/
> **공식 문서:** https://reccobeats.com/docs/documentation/introduction
> **API 기준 버전 표기:** `ReccoBeats API 1.0.0`
> **우리 프로젝트 관점:** `트랙 오디오 특성 보강 후보`

---

## 목차

1. [개요](#개요)
2. [우리 서비스 기준에서 중요한 해석](#우리-서비스-기준에서-중요한-해석)
3. [핵심 엔드포인트](#핵심-엔드포인트)
4. [응답 필드 정리](#응답-필드-정리)
5. [실제 호출로 확인한 동작](#실제-호출로-확인한-동작)
6. [Rate Limit / 오류 처리](#rate-limit--오류-처리)
7. [우리 서비스에 붙일 때의 권장 흐름](#우리-서비스에-붙일-때의-권장-흐름)
8. [현재 코드 기준의 전환 과제](#현재-코드-기준의-전환-과제)

---

## 개요

ReccoBeats는 음악 추천과 음악 데이터베이스 API 서비스다. 공식 소개 문서 기준 특징은 아래와 같다.

- 인증 키 없이 사용 가능
- Base URL은 `https://api.reccobeats.com`
- 음악 추천 API와 트랙/아티스트/앨범 메타데이터 API를 함께 제공
- 트랙 오디오 특성 조회 API와 업로드 기반 오디오 분석 API를 별도로 제공

공식 문서에서 직접 확인된 기본 사실:

- API Access & Authentication: `No API access key or authentication required`
- Base URL: `https://api.reccobeats.com`
- Rate limiting은 존재하지만 숫자 quota는 공개하지 않음

---

## 우리 서비스 기준에서 중요한 해석

ReccoBeats는 우리 서비스에서 두 가지 방식으로 쓸 수 있다.

1. `기존 트랙 조회형`
   - 이미 ReccoBeats DB에 있는 트랙의 오디오 특성을 조회
   - 우리 서비스의 PMS import, EMS 수집 후 보강에 더 가깝다

2. `파일 업로드 분석형`
   - 오디오 파일 일부를 업로드해서 특성을 추출
   - 우리 현재 서비스 흐름에서는 보조 수단에 가깝다

우리 서비스 관점에서 우선 검토해야 할 쪽은 `조회형`이다.  
이유는 `PMS/EMS/GMS`가 기본적으로 외부 플랫폼의 track metadata를 받아 canonical library로 저장하는 구조이기 때문이다.

---

## 핵심 엔드포인트

### 1. 다중 오디오 특성 조회

- `GET /v1/audio-features`
- 전체 URL: `https://api.reccobeats.com/v1/audio-features`

이 엔드포인트는 여러 ID를 한 번에 조회하는 용도다.  
공식 Request/Response 문서 기준으로 배열 파라미터는 아래 두 형식을 지원한다.

- repeated query parameters
  - `?ids=1&ids=2&ids=3`
- comma-separated values
  - `?ids=1,2,3`

이 경로는 현재 우리 서비스에서 가장 유력한 1차 후보다.  
PMS import 시 트랙을 batch로 처리하기 좋고, 실제 호출 결과도 안정적으로 JSON array를 돌려준다.

### 2. 단일 트랙 오디오 특성 조회

- `GET /v1/track/:id/audio-features`
- 전체 URL 예시: `https://api.reccobeats.com/v1/track/{reccobeats_track_id}/audio-features`

공식 문서상 단일 트랙용 경로다.  
실제 호출로 확인해보면 이 `:id`는 Spotify track id가 아니라 `ReccoBeats 내부 track id(UUID)`를 기대한다.

### 3. 트랙 메타데이터 조회

- `GET /v1/track`
- 전체 URL: `https://api.reccobeats.com/v1/track`

이 엔드포인트는 오디오 특성 자체보다 `ReccoBeats 내부 track id`, `trackTitle`, `artists`, `durationMs`, `isrc`, `href`, `popularity`를 확인할 때 중요하다.

우리 서비스에서는 아래 경우에 필요하다.

- Spotify track id는 있는데 ReccoBeats 내부 UUID도 같이 저장하고 싶을 때
- ISRC로 조회했더니 여러 후보가 나와서 title/artist/duration으로 다시 골라야 할 때
- popularity 같은 추가 메타데이터를 함께 쓰고 싶을 때

### 4. 업로드 기반 오디오 특성 추출

- `POST /v1/analysis/audio-features`
- 전체 URL: `https://api.reccobeats.com/v1/analysis/audio-features`
- `Content-Type`: `multipart/form-data`
- body field: `audioFile`

공식 분석 문서 기준 제약:

- 최대 파일 크기: `5MB`
- 공식 문서 본문 기준 지원 포맷: `MP3`, `OGG`, `Vorbis`, `AIFF/AIFC`, `WAV`
- 최대 분석 길이: `30초`

주의:

- 공식 `changelog`의 `2026-03-09` 항목에는 `AAC`, `M4A`, `MP4` 추가가 적혀 있다
- 하지만 분석 문서 본문은 아직 이전 포맷 목록으로 남아 있다
- 즉, 문서 간 불일치가 있어 실제 운영 전에는 샘플 파일로 재검증이 필요하다

---

## 응답 필드 정리

### 조회형 audio-features 응답

실제 조회 응답에서 확인된 주요 필드:

- `id`
  - ReccoBeats 내부 track UUID
- `href`
  - Spotify track URL
- `isrc`
- `acousticness`
- `danceability`
- `energy`
- `instrumentalness`
- `key`
- `liveness`
- `loudness`
- `mode`
- `speechiness`
- `tempo`
- `valence`

중요한 점:

- `key`, `mode`, `isrc`, `href`는 현재 ReccoBeats 조회형 응답에 포함된다
- 반면 현재 코드가 가진 legacy `spotify_*` 저장 필드와 완전히 1:1 대응되지는 않는다

### track 메타데이터 응답

실제 track 조회 응답에서 확인된 주요 필드:

- `id`
- `trackTitle`
- `artists[]`
- `durationMs`
- `isrc`
- `ean`
- `upc`
- `href`
- `availableCountries`
- `popularity`

즉, ReccoBeats를 붙일 때는 오디오 특성 응답 하나만으로는 부족할 수 있다.  
`durationMs`, `popularity`, title 검증이 필요하면 `GET /v1/track`도 같이 쓰는 편이 낫다.

---

## 실제 호출로 확인한 동작

아래는 2026-05-06에 실제 endpoint 호출로 확인한 결과다.  
이 항목들은 공식 문서에 명시되지 않거나 애매한 부분을 보강하기 위한 메모다.

### 1. `/v1/audio-features`는 Spotify track id를 받을 수 있다

실제 호출:

```text
GET /v1/audio-features?ids=00aqkszH1FdUiJJWvX6iEl
```

결과:

- `content` array로 응답
- 내부 `id`는 ReccoBeats UUID로 반환
- `href`는 Spotify track URL로 반환

해석:

- Spotify import 경로에서는 Spotify track id를 바로 넣어 batch 조회할 수 있다

### 2. `/v1/track/:id/audio-features`는 Spotify track id를 받지 않는다

실제 호출:

```text
GET /v1/track/00aqkszH1FdUiJJWvX6iEl/audio-features
```

실제 응답:

- `4041 ResourceNotFoundException`

반면 아래 호출은 성공했다.

```text
GET /v1/track/8212bab8-5911-48a0-b177-24923ef2329a/audio-features
```

해석:

- 단일 endpoint는 ReccoBeats UUID를 기대한다
- Spotify id만 알고 있다면 먼저 `/v1/audio-features` 또는 `/v1/track`로 resolve하는 쪽이 낫다

### 3. ISRC 조회는 가능하지만 단일 매칭을 보장하지 않는다

실제 호출:

```text
GET /v1/audio-features?ids=USUM72104140
GET /v1/track?ids=USUM72104140
```

결과:

- 같은 `ISRC`에 대해 2개의 track record가 반환되었다
- title은 같지만 Spotify href와 popularity가 다른 버전이 함께 나왔다

해석:

- ISRC만으로는 “한 곡 = 한 record”라고 가정하면 위험하다
- ISRC 기반 조회를 쓰려면 최소한 아래 추가 비교가 필요하다
  - `trackTitle`
  - `artist`
  - `durationMs`
  - 필요하면 `href`

### 4. 조회형과 업로드형은 응답 모델이 다르다

- 조회형 `/v1/audio-features`, `/v1/track/:id/audio-features`
  - `id`, `href`, `isrc`, `key`, `mode` 포함
- 업로드형 `/v1/analysis/audio-features`
  - 문서상 `acousticness`, `danceability`, `energy`, `instrumentalness`, `liveness`, `loudness`, `speechiness`, `tempo`, `valence`만 명시

해석:

- 두 API를 같은 스키마로 저장하려면 별도 정규화 계층이 필요하다

---

## Rate Limit / 오류 처리

공식 문서 기준:

- 숫자로 된 rate limit quota는 공개되지 않음
- `429 Too Many Requests`를 받을 수 있음
- `Retry-After` header 확인을 권장

공식 문서에 나온 HTTP / 오류 코드:

- `200 OK`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `429 Too Many Requests`
- `500 Internal Server Error`

대표 오류 코드:

- `4001`: required parameter 누락
- `4002`: malformed request
- `4004`: validation error
- `4041`: resource not found
- `4042`: wrong URL path
- `4291`: too many requests

우리 서비스 쪽 권장사항:

- batch 요청은 캐시를 기본으로 둘 것
- 429 재시도는 즉시 반복하지 말고 지수 backoff 또는 `Retry-After` 우선
- ISRC 중복 응답은 `not found`와 다른 종류의 처리로 분기할 것

---

## 우리 서비스에 붙일 때의 권장 흐름

### Spotify import 경로

1. 플랫폼에서 track metadata를 가져온다
2. Spotify track id가 있으면 `GET /v1/audio-features?ids=...`를 batch 호출한다
3. 응답의 `id`를 `reccobeats_track_id`로 따로 저장한다
4. 필요하면 `GET /v1/track?ids=...`로 `durationMs`, `popularity`를 보강한다

### TIDAL / 타 플랫폼 경로

1. 원본 메타데이터에서 `ISRC` 확보를 우선 시도한다
2. `GET /v1/audio-features?ids={ISRC}` 또는 `GET /v1/track?ids={ISRC}`로 후보를 가져온다
3. 후보가 여러 개면 아래 값으로 좁힌다
   - title
   - artist
   - duration
4. 선택된 record의 `id`를 기준으로 저장한다

### 업로드 분석 경로

이 경로는 현재 우리 서비스의 기본 PMS import 경로보다는 아래 상황에서만 검토하는 편이 맞다.

- DB lookup에 없는 음원
- 로컬 파일 기반 분석이 필요한 관리자용 배치
- EMS 수집 이후 파일 조각으로 fallback 분석을 돌리는 별도 작업

---

## 현재 코드 기준의 전환 과제

`2026-05-06` 기준으로 핵심 제품 문서와 저장 정책 문서는 `provider-neutral` 방향으로 갱신되었다.

하지만 현재 코드와 스키마는 아직 `spotify_*` 이름의 legacy 호환 구조를 사용한다.  
따라서 ReccoBeats를 실제 공급원으로 붙일 때 남는 과제는 아래와 같다.

- ReccoBeats 조회형 응답에는 `time_signature`가 없다
- `spotify_analysis_url`, `spotify_track_href`, `spotify_feature_type`, `spotify_resolved_at` 같은 legacy 필드는 그대로 1:1 매핑되지 않을 수 있다
- 업로드형 분석 응답은 조회형보다 더 적은 필드만 준다
- ISRC는 단일 unique key로 믿기 어렵다

즉, 현재 단계의 실무 과제는 아래 두 가지다.

1. 당분간 `spotify_*` 필드를 `legacy compatibility container`로 유지한다
2. 이후 `provider-neutral audio_feature_*` 구조로 schema/API rename migration을 진행한다

현재 기준 문서는 아래 문서와 함께 읽는 것이 맞다.

- [AUDIO_FEATURE_PROVIDER_STRATEGY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/AUDIO_FEATURE_PROVIDER_STRATEGY.md)
- [PMS_TRACK_AUDIO_FEATURE_STORAGE.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_TRACK_AUDIO_FEATURE_STORAGE.md)

---

## 공식 참고

- ReccoBeats Home:
  https://reccobeats.com/
- Introduction:
  https://reccobeats.com/docs/documentation/introduction
- Request and Response:
  https://reccobeats.com/docs/documentation/request-and-response
- Rate Limiting:
  https://reccobeats.com/docs/documentation/rate-limiting
- Track audio features:
  https://reccobeats.com/docs/apis/get-track-audio-features
- Multiple audio features:
  https://reccobeats.com/docs/apis/get-audio-features
- Audio feature extraction:
  https://reccobeats.com/docs/documentation/Analysis/audio-features-extraction
- Changelog:
  https://reccobeats.com/docs/changelog
