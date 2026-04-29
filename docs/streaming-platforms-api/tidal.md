# TIDAL Web API Reference

> **최종 업데이트:** 2026-04-28  
> **API 버전:** Open API v2  
> **공식 문서:** https://developer.tidal.com

---

## 목차

1. [개요](#개요)
2. [시작하기](#시작하기)
3. [인증 (Authentication)](#인증)
4. [API 규격 (JSON:API)](#api-규격)
5. [카탈로그 엔드포인트](#카탈로그-엔드포인트)
6. [사용자 컬렉션 엔드포인트](#사용자-컬렉션-엔드포인트)
7. [검색 (Search)](#검색)
8. [재생 (Playback)](#재생)
9. [에러 처리](#에러-처리)
10. [Rate Limiting](#rate-limiting)
11. [SDK](#sdk)
12. [개발자 권장사항](#개발자-권장사항)

---

## 개요

TIDAL Web API는 TIDAL 음악 카탈로그(트랙, 앨범, 아티스트, 비디오 등)에 접근하고, 사용자의 컬렉션을 관리할 수 있는 **JSON:API 규격 준수** RESTful API이다.

- **Base URL:** `https://openapi.tidal.com/v2`
- **응답 형식:** JSON:API (application/vnd.api+json)
- **인증:** OAuth 2.1
- **필수 파라미터:** `countryCode` (대부분의 요청에 필수)

### Spotify API와의 주요 차이점

| 항목 | TIDAL | Spotify |
|------|-------|---------|
| API 규격 | JSON:API 준수 | 일반 REST/JSON |
| OAuth 버전 | OAuth 2.1 | OAuth 2.0 |
| 재생 방식 | SDK Player 모듈 전용 | Web Playback SDK / Connect API |
| 응답 구조 | `data`, `included`, `links`, `meta` | 단순 JSON 객체 |
| 관계 데이터 | `include` 파라미터로 side-loading | 별도 요청 필요 |
| 비디오 지원 | ✅ 비디오 엔드포인트 제공 | ❌ |

---

## 시작하기

### 1. 앱 등록

1. [TIDAL Developer Portal](https://developer.tidal.com/) 접속
2. 계정 생성 및 로그인
3. Developer Dashboard에서 새 앱 등록
4. `Client ID`와 `Client Secret` 발급

### 2. 기본 요청 구조

모든 API 요청에는 인증 헤더와 `countryCode`가 필요하다.

```http
GET https://openapi.tidal.com/v2/tracks/{id}?countryCode=US
Authorization: Bearer {ACCESS_TOKEN}
Accept: application/vnd.api+json
```

---

## 인증

TIDAL은 **OAuth 2.1**을 사용하며, 공식 SDK의 Auth 모듈 사용을 권장한다.

### 인증 엔드포인트

| 용도 | URL |
|------|-----|
| Authorization | `https://login.tidal.com/authorize` |
| Token | `https://auth.tidal.com/v1/oauth2/token` |

### 1. Client Credentials Flow

사용자 데이터가 필요 없는 카탈로그 조회용이다.

```http
POST https://auth.tidal.com/v1/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials
&client_id={CLIENT_ID}
&client_secret={CLIENT_SECRET}
```

**응답:**
```json
{
  "access_token": "eyJ...",
  "token_type": "Bearer",
  "expires_in": 86400
}
```

### 2. Authorization Code Flow with PKCE

사용자 인증이 필요한 경우 사용한다. TIDAL은 **모든 클라이언트에 PKCE를 강제**한다.

**Step 1 — Authorization 요청:**
```
GET https://login.tidal.com/authorize
  ?client_id={CLIENT_ID}
  &response_type=code
  &redirect_uri={REDIRECT_URI}
  &scope={SCOPES}
  &code_challenge_method=S256
  &code_challenge={CODE_CHALLENGE}
  &state={RANDOM_STATE}
```

**Step 2 — Token 교환:**
```http
POST https://auth.tidal.com/v1/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code={AUTH_CODE}
&redirect_uri={REDIRECT_URI}
&client_id={CLIENT_ID}
&code_verifier={CODE_VERIFIER}
```

### 3. Refresh Token Flow

```http
POST https://auth.tidal.com/v1/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token
&refresh_token={REFRESH_TOKEN}
&client_id={CLIENT_ID}
```

### PKCE 구현 예시

```javascript
function generateCodeVerifier() {
  const array = new Uint8Array(64);
  crypto.getRandomValues(array);
  return btoa(String.fromCharCode(...array))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}

async function generateCodeChallenge(verifier) {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}
```

---

## API 규격

### JSON:API 응답 구조

TIDAL API는 [JSON:API 스펙](https://jsonapi.org/)을 준수한다.

```json
{
  "data": {
    "id": "12345678",
    "type": "tracks",
    "attributes": {
      "title": "Track Name",
      "duration": 240,
      "isrc": "USRC12345678"
    },
    "relationships": {
      "artists": {
        "data": [{ "id": "111", "type": "artists" }]
      },
      "albums": {
        "data": [{ "id": "222", "type": "albums" }]
      }
    }
  },
  "included": [],
  "links": {
    "self": "https://openapi.tidal.com/v2/tracks/12345678"
  }
}
```

### 주요 필드

| 필드 | 설명 |
|------|------|
| `data` | 요청한 주 리소스 (단일 객체 또는 배열) |
| `included` | `include` 파라미터로 요청한 관련 리소스들 |
| `links` | 페이지네이션 링크 (`self`, `next`, `prev`) |
| `meta` | 요청/컬렉션 메타데이터 |

### `include` 파라미터 (Side-loading)

관련 리소스를 한 번의 요청으로 함께 가져올 수 있다.

```http
GET /v2/albums/{id}?countryCode=US&include=items,items.artists
```

- 중첩 관계는 dot notation 사용: `items.artists`
- 쉼표로 여러 관계 지정: `items,items.artists,items.albums`

### 페이지네이션

커서 기반 페이지네이션을 사용한다.

```http
GET /v2/artists/{id}/albums?countryCode=US&page[cursor]={CURSOR_VALUE}
```

응답의 `links.next`에 다음 페이지 커서가 포함된다.

### `countryCode` 파라미터

대부분의 요청에 **필수**이다. TIDAL 카탈로그는 지역별 라이선싱에 따라 가용성이 다르다.

```http
GET /v2/tracks/{id}?countryCode=KR
```

| 코드 | 국가 |
|------|------|
| `US` | 미국 |
| `KR` | 한국 |
| `GB` | 영국 |
| `JP` | 일본 |
| `DE` | 독일 |

---

## 카탈로그 엔드포인트

### 🎵 Tracks

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/tracks/{id}` | 단일 트랙 정보 |
| `GET` | `/tracks/{id}/relationships/artists` | 트랙 아티스트 관계 |
| `GET` | `/tracks/{id}/relationships/albums` | 트랙 앨범 관계 |

**요청 예시:**
```http
GET https://openapi.tidal.com/v2/tracks/12345678?countryCode=US&include=artists,albums
Authorization: Bearer {ACCESS_TOKEN}
Accept: application/vnd.api+json
```

**응답 예시 (주요 attributes):**
```json
{
  "data": {
    "id": "12345678",
    "type": "tracks",
    "attributes": {
      "title": "Song Title",
      "duration": 215,
      "trackNumber": 3,
      "volumeNumber": 1,
      "isrc": "USRC12345678",
      "copyright": "© 2025 Label Name",
      "popularity": 85,
      "availability": ["STREAM", "DJ"],
      "mediaMetadata": {
        "tags": ["HIRES_LOSSLESS"]
      }
    }
  }
}
```

### 💿 Albums

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/albums/{id}` | 단일 앨범 정보 |
| `GET` | `/albums/{id}/relationships/items` | 앨범 트랙 목록 |
| `GET` | `/albums/{id}/relationships/artists` | 앨범 아티스트 관계 |

**요청 예시 (앨범 + 트랙 + 아티스트 한 번에 조회):**
```http
GET https://openapi.tidal.com/v2/albums/{id}?countryCode=US&include=items,items.artists
Authorization: Bearer {ACCESS_TOKEN}
```

### 🎤 Artists

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/artists/{id}` | 단일 아티스트 정보 |
| `GET` | `/artists/{id}/relationships/albums` | 아티스트 앨범 목록 |
| `GET` | `/artists/{id}/relationships/tracks` | 아티스트 트랙 목록 |

### 🎬 Videos

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/videos/{id}` | 단일 비디오 정보 |
| `GET` | `/videos/{id}/relationships/artists` | 비디오 아티스트 관계 |

---

## 사용자 컬렉션 엔드포인트

> ⚠️ 사용자 컬렉션 접근에는 **Authorization Code Flow** 인증이 필요하다.

### 사용자 컬렉션 리소스

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/userCollectionTracks` | 사용자 저장 트랙 |
| `POST` | `/userCollectionTracks/items` | 트랙 저장 |
| `DELETE` | `/userCollectionTracks/items` | 트랙 제거 |
| `GET` | `/userCollectionAlbums` | 사용자 저장 앨범 |
| `POST` | `/userCollectionAlbums/items` | 앨범 저장 |
| `DELETE` | `/userCollectionAlbums/items` | 앨범 제거 |
| `GET` | `/userCollectionPlaylists` | 사용자 플레이리스트 |
| `GET` | `/userCollectionVideos` | 사용자 저장 비디오 |

**요청 예시 — 컬렉션 트랙 조회:**
```http
GET https://openapi.tidal.com/v2/userCollectionTracks?countryCode=US
Authorization: Bearer {USER_ACCESS_TOKEN}
Accept: application/vnd.api+json
```

---

## 검색

### Search 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/searchresults` | 카탈로그 통합 검색 |

### 파라미터

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `query` | ✅ | 검색어 |
| `countryCode` | ✅ | 지역 코드 (예: `US`, `KR`) |
| `type` | ❌ | 검색 타입 필터 (`tracks`, `albums`, `artists`, `videos`, `playlists`) |
| `limit` | ❌ | 결과 수 제한 |
| `offset` | ❌ | 오프셋 (페이지네이션) |
| `include` | ❌ | 관련 리소스 side-loading |

### 요청 예시

**기본 검색:**
```http
GET https://openapi.tidal.com/v2/searchresults?query=BTS&countryCode=KR&type=tracks&limit=20
Authorization: Bearer {ACCESS_TOKEN}
Accept: application/vnd.api+json
```

**관련 리소스 포함 검색:**
```http
GET https://openapi.tidal.com/v2/searchresults?query=NewJeans&countryCode=KR&include=tracks,tracks.artists,tracks.albums
Authorization: Bearer {ACCESS_TOKEN}
```

---

## 재생

> ⚠️ **중요:** TIDAL 콘텐츠 재생은 **공식 SDK의 Player 모듈**을 통해서만 허용된다. 비공식 방법으로의 재생은 약관 위반이다.

### Player 모듈

TIDAL SDK에 포함된 Player 모듈이 유일한 공식 재생 방법이다.

**Web (JavaScript):**
```javascript
import { TidalPlayer } from '@tidal-music/player';

const player = new TidalPlayer({
  clientId: 'YOUR_CLIENT_ID',
});

// 트랙 재생
await player.load('track/12345678');
await player.play();

// 일시정지
await player.pause();
```

### Player 주요 기능

| 기능 | 설명 |
|------|------|
| `load()` | 트랙/비디오 로드 |
| `play()` | 재생 시작/재개 |
| `pause()` | 일시정지 |
| `next()` | 다음 트랙 |
| `previous()` | 이전 트랙 |
| `seek()` | 재생 위치 이동 |
| `setVolume()` | 볼륨 조절 |

### 오디오 품질

TIDAL은 다양한 오디오 품질을 지원한다.

| 품질 | 포맷 | 비트레이트 |
|------|------|-----------|
| Normal | AAC | 96 kbps |
| High | AAC | 320 kbps |
| HiFi (Lossless) | FLAC | 1411 kbps (CD 품질) |
| Max (Hi-Res) | FLAC | 최대 9216 kbps (24bit/192kHz) |
| Dolby Atmos | Dolby Digital Plus | 768 kbps |
| Sony 360 Reality Audio | MQA/FLAC | 다양 |

---

## 에러 처리

### HTTP 상태 코드

| 코드 | 의미 | 대응 방법 |
|------|------|-----------|
| `200` | 성공 | 정상 처리 |
| `201` | 생성됨 | 리소스 생성 성공 |
| `204` | No Content | 삭제 성공 |
| `400` | Bad Request | 요청 파라미터 확인 (countryCode 누락 등) |
| `401` | Unauthorized | 토큰 만료 → refresh token으로 갱신 |
| `403` | Forbidden | scope 부족 또는 구독 등급 부족 |
| `404` | Not Found | 리소스 ID 확인 |
| `429` | Too Many Requests | `Retry-After` 헤더 확인 후 대기 |
| `500` | Server Error | 재시도 (exponential backoff) |

### 에러 응답 형식 (JSON:API)

```json
{
  "errors": [
    {
      "status": "401",
      "title": "Unauthorized",
      "detail": "The access token expired"
    }
  ]
}
```

### 자동 토큰 갱신 예시

```javascript
async function tidalRequest(url, options = {}) {
  let response = await fetch(url, {
    ...options,
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Accept': 'application/vnd.api+json',
      ...options.headers,
    },
  });

  // 401이면 토큰 갱신 후 재시도
  if (response.status === 401) {
    await refreshAccessToken();
    response = await fetch(url, {
      ...options,
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Accept': 'application/vnd.api+json',
        ...options.headers,
      },
    });
  }

  // 429이면 Retry-After 대기
  if (response.status === 429) {
    const retryAfter = parseInt(response.headers.get('Retry-After') || '1');
    await new Promise(r => setTimeout(r, retryAfter * 1000));
    return tidalRequest(url, options); // 재귀 재시도
  }

  return response;
}
```

---

## Rate Limiting

### 정책

- Rate limit 초과 시 `429 Too Many Requests` 응답
- `Retry-After` 헤더에 대기 시간(초) 포함
- 정확한 한도는 공개되지 않음 (앱 등급에 따라 다름)

### 최소화 전략

1. **`include` 파라미터 활용** — 여러 관계를 한 번의 요청으로 조회
2. **캐싱** — 카탈로그 메타데이터는 적극적으로 캐싱
3. **Exponential Backoff** — 429 응답 시 지수적 대기
4. **요청 큐잉** — 동시 요청 수 제한
5. **커서 페이지네이션** — 대량 데이터 조회 시 커서 활용

---

## SDK

TIDAL은 Web, Android, iOS용 공식 SDK를 제공한다.

### SDK 모듈 구성

| 모듈 | 설명 |
|------|------|
| **Auth** | OAuth 2.1 인증 흐름 관리 (토큰 저장, 자동 갱신) |
| **Player** | TIDAL 콘텐츠 재생 (유일한 공식 재생 방법) |
| **Catalogue** | 카탈로그 검색 및 메타데이터 조회 |

### 플랫폼별 SDK

| 플랫폼 | 패키지 |
|--------|--------|
| Web (JS/TS) | `@tidal-music/auth`, `@tidal-music/player` |
| Android | `com.tidal.sdk:auth`, `com.tidal.sdk:player` |
| iOS | `TidalAuth`, `TidalPlayer` (Swift Package) |

### SDK 설치 (Web)

```bash
npm install @tidal-music/auth @tidal-music/player
```

### SDK 인증 예시 (Web)

```javascript
import { init as initAuth, credentialsProvider } from '@tidal-music/auth';

// Client Credentials 초기화
await initAuth({
  clientId: 'YOUR_CLIENT_ID',
  clientSecret: 'YOUR_CLIENT_SECRET',
});

// API 요청 시 credentials 사용
const credentials = await credentialsProvider.getCredentials();
const response = await fetch(
  'https://openapi.tidal.com/v2/tracks/12345678?countryCode=US',
  {
    headers: {
      'Authorization': `Bearer ${credentials.token}`,
      'Accept': 'application/vnd.api+json',
    },
  }
);
```

### GitHub 리포지토리

| 플랫폼 | URL |
|--------|-----|
| Web SDK | https://github.com/tidal-music/tidal-sdk-web |
| Android SDK | https://github.com/tidal-music/tidal-sdk-android |
| iOS SDK | https://github.com/tidal-music/tidal-sdk-ios |

---

## 개발자 권장사항

### ✅ Do

- **공식 SDK 사용** — Auth, Player 모듈 적극 활용
- **`include` 파라미터 활용** — API 호출 수 최소화
- **`countryCode` 항상 포함** — 누락 시 에러 또는 불완전한 데이터
- **커서 페이지네이션 사용** — 대량 데이터 효율적 조회
- **토큰 자동 갱신 구현** — 401 에러에 대한 자동 처리
- **`Retry-After` 준수** — 429 에러 시 헤더 값 대기
- **JSON:API Accept 헤더** — `application/vnd.api+json` 사용

### ❌ Don't

- **비공식 엔드포인트 사용 금지** — `listen.tidal.com` 등 내부 API 사용 금지
- **SDK 외 재생 구현 금지** — Player 모듈만 재생 허용
- **Client Secret 노출 금지** — 프론트엔드 코드에 포함하지 않기
- **과도한 요청 금지** — 스크래핑 패턴 지양
- **`countryCode` 하드코딩 금지** — 사용자 지역에 맞게 동적 설정

### 프로젝트 적용 체크리스트

- [ ] Developer Portal에서 앱 등록 및 Client ID/Secret 발급
- [ ] 인증 Flow 선택 (카탈로그만: Client Credentials / 사용자 데이터: PKCE)
- [ ] SDK 설치 및 Auth 모듈 초기화
- [ ] `countryCode` 설정 로직 구현
- [ ] 에러 핸들링 구현 (401 토큰 갱신, 429 backoff)
- [ ] Player 모듈 연동 (재생 기능 필요 시)
- [ ] 캐싱 전략 수립

---

## 참고 링크

| 리소스 | URL |
|--------|-----|
| Developer Portal | https://developer.tidal.com |
| API Reference | https://developer.tidal.com/documentation/api |
| Auth Guide | https://developer.tidal.com/documentation/authorization |
| Developer Guidelines | https://developer.tidal.com/documentation/guidelines |
| Web SDK (GitHub) | https://github.com/tidal-music/tidal-sdk-web |
| Android SDK (GitHub) | https://github.com/tidal-music/tidal-sdk-android |
| iOS SDK (GitHub) | https://github.com/tidal-music/tidal-sdk-ios |
| JSON:API Spec | https://jsonapi.org |
