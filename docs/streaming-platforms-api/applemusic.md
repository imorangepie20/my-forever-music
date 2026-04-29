# Apple Music API Reference

> **최종 업데이트:** 2026-04-28  
> **API 버전:** v1  
> **공식 문서:** https://developer.apple.com/documentation/applemusicapi

---

## 목차

1. [개요](#개요)
2. [시작하기](#시작하기)
3. [인증 (Authentication)](#인증)
4. [카탈로그 엔드포인트](#카탈로그-엔드포인트)
5. [검색 (Search)](#검색)
6. [사용자 라이브러리 엔드포인트](#사용자-라이브러리-엔드포인트)
7. [개인화 엔드포인트](#개인화-엔드포인트)
8. [재생 (MusicKit)](#재생)
9. [에러 처리](#에러-처리)
10. [Rate Limiting](#rate-limiting)
11. [개발자 권장사항](#개발자-권장사항)

---

## 개요

Apple Music API는 Apple Music 카탈로그(곡, 앨범, 아티스트, 뮤직비디오, 플레이리스트 등)에 접근하고 사용자의 iCloud Music Library를 관리할 수 있는 RESTful API이다.

- **Base URL:** `https://api.music.apple.com/v1`
- **응답 형식:** JSON
- **인증:** Developer Token (JWT) + Music User Token
- **필수 개념:** Storefront (지역 코드, 예: `us`, `kr`, `jp`)

### 다른 플랫폼 API와의 주요 차이점

| 항목 | Apple Music | Spotify | TIDAL |
|------|-------------|---------|-------|
| 인증 방식 | JWT Developer Token + Music User Token | OAuth 2.0 | OAuth 2.1 |
| API 규격 | REST/JSON | REST/JSON | JSON:API |
| 재생 방식 | MusicKit (JS/Swift/Android) | Web Playback SDK | SDK Player 모듈 |
| 지역 개념 | Storefront (`/catalog/{storefront}/`) | `market` 파라미터 | `countryCode` 파라미터 |
| 비디오 지원 | ✅ 뮤직비디오 엔드포인트 | ❌ | ✅ |
| 무료 계정 접근 | 카탈로그 조회 가능 (미리듣기) | 카탈로그 조회 가능 | 카탈로그 조회 가능 |

---

## 시작하기

### 1. 사전 요구사항

- **Apple Developer Program** 멤버십 (연 $99)
- Apple Developer 계정에서 **MusicKit 키** 생성

### 2. MusicKit 키 생성

1. [Apple Developer](https://developer.apple.com/account/) → Certificates, Identifiers & Profiles → Keys
2. "+" 클릭 → **MusicKit** 체크 → 키 생성
3. `.p8` 파일 다운로드 (한 번만 다운로드 가능)
4. **Key ID** (10자 식별자) 및 **Team ID** 기록

### 3. 기본 요청 구조

```http
GET https://api.music.apple.com/v1/catalog/us/songs/203709340
Authorization: Bearer {DEVELOPER_TOKEN}
```

사용자 데이터 접근 시:
```http
GET https://api.music.apple.com/v1/me/library/songs
Authorization: Bearer {DEVELOPER_TOKEN}
Music-User-Token: {MUSIC_USER_TOKEN}
```

---

## 인증

Apple Music API는 두 가지 토큰을 사용한다.

### 1. Developer Token (JWT)

앱을 인증하는 JWT 토큰이다. **모든 API 요청에 필수.**

**JWT Header:**
```json
{
  "alg": "ES256",
  "kid": "{KEY_ID}"
}
```

**JWT Payload:**
```json
{
  "iss": "{TEAM_ID}",
  "iat": 1714300000,
  "exp": 1729852000,
  "origin": ["https://your-app.com"]
}
```

| Claim | 설명 |
|-------|------|
| `iss` | Apple Developer Team ID (10자) |
| `iat` | 토큰 발급 시간 (Unix timestamp) |
| `exp` | 만료 시간 (최대 발급일로부터 **6개월**) |
| `origin` | (선택) 웹 클라이언트 origin 배열 — 무단 사용 방지 |

**Node.js 생성 예시:**
```javascript
const jwt = require('jsonwebtoken');
const fs = require('fs');

const privateKey = fs.readFileSync('AuthKey_{KEY_ID}.p8');

const token = jwt.sign({}, privateKey, {
  algorithm: 'ES256',
  expiresIn: '180d', // 최대 6개월
  issuer: 'YOUR_TEAM_ID',
  header: {
    alg: 'ES256',
    kid: 'YOUR_KEY_ID'
  }
});
```

**Python 생성 예시:**
```python
import jwt
import time

private_key = open('AuthKey_{KEY_ID}.p8').read()

token = jwt.encode(
    payload={
        'iss': 'YOUR_TEAM_ID',
        'iat': int(time.time()),
        'exp': int(time.time()) + (180 * 24 * 60 * 60),  # 6개월
    },
    key=private_key,
    algorithm='ES256',
    headers={
        'kid': 'YOUR_KEY_ID'
    }
)
```

### 2. Music User Token

사용자 개인 데이터 접근에 필요한 토큰이다. MusicKit을 통해 자동으로 관리된다.

**MusicKit JS (웹):**
```javascript
const music = MusicKit.getInstance();
const musicUserToken = await music.authorize(); // Apple 로그인 팝업
```

**MusicKit for Swift (iOS):**
```swift
let status = await MusicAuthorization.request()
// 승인되면 MusicKit이 자동으로 토큰 관리
```

### 요청 헤더 정리

| 헤더 | 필수 여부 | 설명 |
|------|-----------|------|
| `Authorization: Bearer {DEV_TOKEN}` | **항상 필수** | Developer Token |
| `Music-User-Token: {USER_TOKEN}` | `/me` 엔드포인트에 필수 | 사용자 인증 토큰 |

---

## 카탈로그 엔드포인트

카탈로그 엔드포인트는 Developer Token만으로 접근 가능하다.

**Base Path:** `/v1/catalog/{storefront}/`

### 🎵 Songs

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/catalog/{storefront}/songs/{id}` | 단일 곡 조회 |
| `GET` | `/catalog/{storefront}/songs` | 여러 곡 조회 (`?ids=id1,id2,...`) |

**요청 예시:**
```http
GET https://api.music.apple.com/v1/catalog/kr/songs/203709340
Authorization: Bearer {DEVELOPER_TOKEN}
```

**응답 예시:**
```json
{
  "data": [
    {
      "id": "203709340",
      "type": "songs",
      "attributes": {
        "name": "Song Title",
        "artistName": "Artist Name",
        "albumName": "Album Name",
        "durationInMillis": 215000,
        "releaseDate": "2025-01-15",
        "isrc": "USRC12345678",
        "genreNames": ["K-Pop"],
        "trackNumber": 3,
        "discNumber": 1,
        "hasLyrics": true,
        "previews": [
          { "url": "https://audio-ssl.itunes.apple.com/..." }
        ],
        "artwork": {
          "url": "https://is1-ssl.mzstatic.com/.../{w}x{h}bb.jpg",
          "width": 3000,
          "height": 3000
        }
      },
      "relationships": {
        "artists": { "data": [{ "id": "111", "type": "artists" }] },
        "albums": { "data": [{ "id": "222", "type": "albums" }] }
      }
    }
  ]
}
```

### 💿 Albums

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/catalog/{storefront}/albums/{id}` | 단일 앨범 조회 |
| `GET` | `/catalog/{storefront}/albums` | 여러 앨범 조회 |

**관계 데이터 포함 조회:**
```http
GET /v1/catalog/us/albums/{id}?include=tracks,artists
```

### 🎤 Artists

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/catalog/{storefront}/artists/{id}` | 단일 아티스트 조회 |
| `GET` | `/catalog/{storefront}/artists/{id}/albums` | 아티스트 앨범 목록 |
| `GET` | `/catalog/{storefront}/artists/{id}/songs` | 아티스트 곡 목록 |
| `GET` | `/catalog/{storefront}/artists/{id}/music-videos` | 아티스트 뮤직비디오 |

### 🎬 Music Videos

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/catalog/{storefront}/music-videos/{id}` | 단일 뮤직비디오 조회 |
| `GET` | `/catalog/{storefront}/music-videos` | 여러 뮤직비디오 조회 |

### 📋 Playlists

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/catalog/{storefront}/playlists/{id}` | 단일 플레이리스트 조회 |
| `GET` | `/catalog/{storefront}/playlists` | 여러 플레이리스트 조회 |

### 📻 Stations

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/catalog/{storefront}/stations/{id}` | 단일 스테이션 조회 |

### 🏷️ Genres

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/catalog/{storefront}/genres` | 전체 장르 목록 |
| `GET` | `/catalog/{storefront}/genres/{id}` | 단일 장르 정보 |

### 🌍 Storefronts

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/storefronts` | 전체 Storefront 목록 |
| `GET` | `/storefronts/{id}` | 단일 Storefront 정보 |

**주요 Storefront 코드:**

| 코드 | 국가 |
|------|------|
| `us` | 미국 |
| `kr` | 한국 |
| `jp` | 일본 |
| `gb` | 영국 |
| `de` | 독일 |

### 공통 쿼리 파라미터

| 파라미터 | 설명 | 예시 |
|----------|------|------|
| `l` | 로컬라이제이션 (언어) | `l=ko` |
| `include` | 관계 데이터 포함 | `include=tracks,artists` |
| `extend` | 속성 확장 | `extend=editorialNotes` |
| `limit` | 결과 수 제한 | `limit=25` |
| `offset` | 오프셋 (페이지네이션) | `offset=25` |

---

## 검색

### Search 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/catalog/{storefront}/search` | 카탈로그 통합 검색 |
| `GET` | `/catalog/{storefront}/search/hints` | 검색 자동완성 힌트 |

### 파라미터

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `term` | ✅ | 검색어 (공백은 `+`로 대체) |
| `types` | ❌ | 검색 타입 (쉼표 구분): `songs`, `albums`, `artists`, `playlists`, `music-videos`, `stations` |
| `limit` | ❌ | 타입별 결과 수 제한 (기본 5, 최대 25) |
| `offset` | ❌ | 타입별 오프셋 |
| `l` | ❌ | 로컬라이제이션 |

### 요청 예시

```http
GET https://api.music.apple.com/v1/catalog/kr/search?term=NewJeans&types=songs,albums,artists&limit=10
Authorization: Bearer {DEVELOPER_TOKEN}
```

**자동완성:**
```http
GET https://api.music.apple.com/v1/catalog/kr/search/hints?term=New&types=songs,artists
Authorization: Bearer {DEVELOPER_TOKEN}
```

---

## 사용자 라이브러리 엔드포인트

> ⚠️ 모든 `/me` 엔드포인트는 **Developer Token + Music User Token** 필수

**Base Path:** `/v1/me/`

### 라이브러리 조회

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/me/library/songs` | 라이브러리 곡 목록 |
| `GET` | `/me/library/songs/{id}` | 라이브러리 단일 곡 |
| `GET` | `/me/library/albums` | 라이브러리 앨범 목록 |
| `GET` | `/me/library/albums/{id}` | 라이브러리 단일 앨범 |
| `GET` | `/me/library/artists` | 라이브러리 아티스트 목록 |
| `GET` | `/me/library/playlists` | 라이브러리 플레이리스트 목록 |
| `GET` | `/me/library/playlists/{id}` | 라이브러리 단일 플레이리스트 |
| `GET` | `/me/library/music-videos` | 라이브러리 뮤직비디오 |

### 라이브러리 추가

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/me/library` | 카탈로그 리소스를 라이브러리에 추가 |

**요청 예시 — 곡 추가:**
```http
POST https://api.music.apple.com/v1/me/library?ids[songs]=203709340,203709341
Authorization: Bearer {DEVELOPER_TOKEN}
Music-User-Token: {MUSIC_USER_TOKEN}
```

### 플레이리스트 관리

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/me/library/playlists` | 새 플레이리스트 생성 |
| `POST` | `/me/library/playlists/{id}/tracks` | 플레이리스트에 트랙 추가 |

**플레이리스트 생성 예시:**
```http
POST https://api.music.apple.com/v1/me/library/playlists
Authorization: Bearer {DEVELOPER_TOKEN}
Music-User-Token: {MUSIC_USER_TOKEN}
Content-Type: application/json

{
  "attributes": {
    "name": "My Playlist",
    "description": "A great playlist"
  },
  "relationships": {
    "tracks": {
      "data": [
        { "id": "203709340", "type": "songs" },
        { "id": "203709341", "type": "songs" }
      ]
    }
  }
}
```

---

## 개인화 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/me/recent/played` | 최근 재생 항목 |
| `GET` | `/me/recommendations` | 개인 추천 |
| `GET` | `/me/storefront` | 사용자 Storefront 정보 |

---

## 재생

### MusicKit JS (웹)

Apple Music 콘텐츠 재생은 **MusicKit** 프레임워크를 통해서만 가능하다.

**스크립트 로드:**
```html
<script src="https://js-cdn.music.apple.com/musickit/v3/musickit.js"
        data-web-components async></script>
```

**초기화:**
```javascript
document.addEventListener('musickitloaded', async () => {
  await MusicKit.configure({
    developerToken: 'YOUR_JWT_DEVELOPER_TOKEN',
    app: {
      name: 'My Music App',
      build: '1.0.0'
    }
  });
});
```

**사용자 인증 및 재생:**
```javascript
const music = MusicKit.getInstance();

// 사용자 인증 (Apple ID 로그인 팝업)
await music.authorize();

// 앨범 재생
await music.setQueue({ album: 'ALBUM_ID' });
await music.play();

// 개별 곡 재생
await music.setQueue({ song: 'SONG_ID' });
await music.play();
```

### Player 제어

```javascript
const player = music.player;

player.play();           // 재생
player.pause();          // 일시정지
player.stop();           // 정지
player.skipToNextItem(); // 다음 트랙
player.skipToPreviousItem(); // 이전 트랙
player.seekToTime(30);   // 30초로 이동
player.volume = 0.5;     // 볼륨 (0.0 ~ 1.0)
```

### 재생 이벤트

```javascript
music.addEventListener('playbackStateDidChange', (event) => {
  console.log('State:', event.state); // playing, paused, stopped, etc.
});

music.addEventListener('nowPlayingItemDidChange', (event) => {
  console.log('Now playing:', event.item);
});

music.addEventListener('playbackTimeDidChange', (event) => {
  console.log('Current time:', event.currentPlaybackTime);
});
```

### 미리듣기 vs 전체 재생

| 조건 | 재생 |
|------|------|
| Developer Token만 (미인증 사용자) | **30초 미리듣기**만 가능 |
| Developer Token + Music User Token (구독자) | **전체 곡 재생** 가능 |

---

## 에러 처리

### HTTP 상태 코드

| 코드 | 의미 | 대응 방법 |
|------|------|-----------|
| `200` | 성공 | 정상 처리 |
| `201` | 생성됨 | 리소스 생성 성공 |
| `204` | No Content | 삭제/업데이트 성공 |
| `400` | Bad Request | 요청 파라미터 확인 |
| `401` | Unauthorized | Developer Token 만료 → 재생성 |
| `403` | Forbidden | Music User Token 만료 또는 권한 부족 |
| `404` | Not Found | 리소스 ID 또는 Storefront 확인 |
| `429` | Too Many Requests | Rate limit 초과 → backoff |
| `500` | Server Error | 재시도 |

### 에러 응답 형식

```json
{
  "errors": [
    {
      "id": "ABCDEF",
      "title": "Resource Not Found",
      "detail": "Resource with requested id was not found",
      "status": "404",
      "code": "40400"
    }
  ]
}
```

---

## Rate Limiting

### 정책

- 요청량은 **Developer Token 기준**으로 모니터링
- 초과 시 `429 Too Many Requests` 반환
- `Retry-After` 헤더에 대기 시간 포함될 수 있음
- 정확한 수치는 공개되지 않으며 Apple이 동적으로 조정

### 요청 예시 — Rate Limit 핸들링

```javascript
async function appleMusicRequest(url, headers, maxRetries = 3) {
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    const response = await fetch(url, { headers });

    if (response.status === 429) {
      const retryAfter = parseInt(response.headers.get('Retry-After') || '5');
      console.warn(`Rate limited. Waiting ${retryAfter}s...`);
      await new Promise(r => setTimeout(r, retryAfter * 1000));
      continue;
    }

    if (response.status === 401) {
      // Developer Token 만료 — 재생성 필요
      throw new Error('Developer token expired. Regenerate JWT.');
    }

    return response;
  }

  throw new Error('Max retries exceeded');
}
```

### 최소화 전략

1. **`include` 파라미터 활용** — 관계 데이터를 한 번에 조회
2. **캐싱** — 카탈로그 메타데이터 적극 캐시
3. **Exponential Backoff** — 429 시 지수적 대기
4. **배치 요청** — `?ids=id1,id2,...` 형태로 일괄 조회
5. **페이지네이션** — `limit`/`offset` 적절히 활용

---

## 개발자 권장사항

### ✅ Do

- **Developer Token은 서버에서 생성** — Private Key를 프론트엔드에 노출하지 않기
- **MusicKit 프레임워크 사용** — 인증/재생 자동 관리
- **`include` 파라미터 활용** — API 호출 수 최소화
- **`origin` claim 설정** — 웹 앱의 경우 JWT에 origin 지정
- **Storefront 동적 설정** — 사용자 지역에 맞게 (`/me/storefront`)
- **미리듣기 URL 활용** — 비구독자에게 30초 프리뷰 제공

### ❌ Don't

- **Private Key (.p8) 프론트엔드 포함 금지** — 보안 위험
- **Developer Token 하드코딩 금지** — 서버에서 동적 생성
- **비공식 방법 재생 금지** — MusicKit만 사용
- **과도한 요청 금지** — 스크래핑 패턴 지양
- **만료된 토큰 방치 금지** — 자동 갱신 로직 구현

### 프로젝트 적용 체크리스트

- [ ] Apple Developer Program 가입
- [ ] MusicKit 키 생성 및 `.p8` 파일 안전한 곳에 보관
- [ ] 서버 사이드 JWT 생성 로직 구현
- [ ] MusicKit JS/Swift/Android 연동
- [ ] 사용자 인증 Flow 구현 (authorize)
- [ ] Storefront 설정 로직 구현
- [ ] 에러 핸들링 구현 (401 토큰 재생성, 429 backoff)
- [ ] 캐싱 전략 수립

---

## 참고 링크

| 리소스 | URL |
|--------|-----|
| Apple Music API 문서 | https://developer.apple.com/documentation/applemusicapi |
| MusicKit 문서 | https://developer.apple.com/documentation/musickit |
| MusicKit JS | https://developer.apple.com/documentation/musickitjs |
| Developer Token 생성 가이드 | https://developer.apple.com/documentation/applemusicapi/generating_developer_tokens |
| Apple Developer 계정 | https://developer.apple.com/account |
| Storefronts 목록 | https://developer.apple.com/documentation/applemusicapi/storefronts |
