# Spotify Web API Reference

> **최종 업데이트:** 2026-04-28  
> **API 버전:** 2026-02 (February 2026 Migration)  
> **공식 문서:** https://developer.spotify.com/documentation/web-api

---

## 목차

1. [개요](#개요)
2. [2026년 주요 변경사항 요약](#2026년-주요-변경사항-요약)
3. [앱 모드: Development vs Extended Quota](#앱-모드)
4. [인증 (Authentication)](#인증)
5. [사용 가능한 엔드포인트](#사용-가능한-엔드포인트)
6. [제거/폐기된 엔드포인트](#제거된-엔드포인트)
7. [Rate Limiting](#rate-limiting)
8. [마이그레이션 가이드](#마이그레이션-가이드)
9. [개발자 권장사항](#개발자-권장사항)

---

## 개요

Spotify Web API는 Spotify 카탈로그 데이터(앨범, 아티스트, 트랙, 플레이리스트 등)에 접근하고, 사용자의 라이브러리와 재생을 제어할 수 있는 RESTful API이다.

- **Base URL:** `https://api.spotify.com/v1`
- **응답 형식:** JSON
- **인증:** OAuth 2.0

> ⚠️ **중요:** 2026년 2월/3월 대규모 정책 변경이 적용되었다. 기존 앱은 반드시 마이그레이션이 필요하다.

---

## 2026년 주요 변경사항 요약

### 정책 변경 (2026년 3월 9일 적용)

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| Premium 요구사항 | 불필요 | Development Mode 앱 소유자 **Premium 필수** |
| Client ID 제한 | 제한 없음 | Development Mode당 **1개**로 제한 |
| 사용자 수 제한 | 제한 없음 | Development Mode 앱당 **최대 5명** |
| 엔드포인트 접근 | 전체 접근 가능 | Development Mode는 **제한된 엔드포인트**만 접근 가능 |
| Search limit 파라미터 | max=50, default=20 | max=**10**, default=**5** |

### 엔드포인트 구조 변경 (2026년 2월)

- **라이브러리 관리:** 개별 타입별 엔드포인트 → 통합 `/me/library` 엔드포인트로 대체
- **플레이리스트:** `/playlists/{id}/tracks` → `/playlists/{id}/items`로 대체
- **카탈로그:** 다수의 browse/catalog 엔드포인트 제거
- **Audio Features/Analysis:** Development Mode에서 접근 제한

---

## 앱 모드

### Development Mode

학습, 실험, 개인 프로젝트용 샌드박스 환경이다.

**요구사항:**
- 앱 소유자의 **Spotify Premium 구독 필수** (구독 만료 시 앱 작동 중지)
- **Client ID 1개**로 제한
- **최대 5명**의 인증된 테스트 사용자
- 제한된 엔드포인트 및 필드만 접근 가능

### Extended Quota Mode

상용 서비스용 확장 모드이다. 2026년 2월 변경사항의 **영향을 받지 않는다.**

**신청 요건:**
- Developer Dashboard에서 "Quota Extension Request" 제출
- 런칭된 서비스 + 유의미한 사용자 기반 필요 (일반적으로 MAU 250,000 이상)
- 2025년 5월부터 신규 신청은 사업체에 한해 제한적으로 승인

**신청 방법:**
1. [Developer Dashboard](https://developer.spotify.com/dashboard/) 접속
2. 앱 설정 → "Quota Extension Request" 섹션
3. 설문 작성 및 제출

---

## 인증

### 지원하는 OAuth 2.0 Flow

#### 1. Authorization Code with PKCE (권장)

서버 시크릿 없이 안전한 인증이 가능하다. **SPA, 모바일 앱에 권장.**

```
1. Code Verifier 생성 (43-128자 랜덤 문자열)
2. Code Challenge 생성 (SHA256 → Base64URL)
3. Authorization URL로 리다이렉트
4. Callback에서 authorization code 수신
5. Token 교환 요청
```

**Authorization URL:**
```
GET https://accounts.spotify.com/authorize
  ?client_id={CLIENT_ID}
  &response_type=code
  &redirect_uri={REDIRECT_URI}
  &scope={SCOPES}
  &code_challenge_method=S256
  &code_challenge={CODE_CHALLENGE}
```

**Token 교환:**
```http
POST https://accounts.spotify.com/api/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code={AUTH_CODE}
&redirect_uri={REDIRECT_URI}
&client_id={CLIENT_ID}
&code_verifier={CODE_VERIFIER}
```

#### 2. Authorization Code Flow (서버 사이드)

백엔드가 있는 서비스에 적합하다.

```http
POST https://accounts.spotify.com/api/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic {BASE64(CLIENT_ID:CLIENT_SECRET)}

grant_type=authorization_code
&code={AUTH_CODE}
&redirect_uri={REDIRECT_URI}
```

#### 3. Client Credentials Flow

사용자 데이터가 필요 없는 서버 간 통신용이다.

```http
POST https://accounts.spotify.com/api/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic {BASE64(CLIENT_ID:CLIENT_SECRET)}

grant_type=client_credentials
```

> ⚠️ **Implicit Grant Flow는 폐기(deprecated)되었다.** PKCE Flow로 마이그레이션 필요.

### Token 관리

| 항목 | 값 |
|------|-----|
| Access Token 유효기간 | 1시간 (3600초) |
| Refresh Token | Authorization Code Flow에서만 발급 |
| Token 갱신 | `grant_type=refresh_token` 사용 |

### 주요 OAuth Scopes

#### 사용자 라이브러리

| Scope | 설명 |
|-------|------|
| `user-library-read` | 사용자 라이브러리 읽기 |
| `user-library-modify` | 사용자 라이브러리 수정 |

#### 재생 (Playback)

| Scope | 설명 |
|-------|------|
| `user-read-playback-state` | 현재 재생 상태 읽기 |
| `user-modify-playback-state` | 재생 제어 (재생/일시정지/스킵 등) |
| `user-read-currently-playing` | 현재 재생 중인 트랙 읽기 |
| `user-read-recently-played` | 최근 재생 기록 읽기 |
| `streaming` | Web Playback SDK 사용 |

#### 플레이리스트

| Scope | 설명 |
|-------|------|
| `playlist-read-private` | 비공개 플레이리스트 읽기 |
| `playlist-read-collaborative` | 협업 플레이리스트 읽기 |
| `playlist-modify-public` | 공개 플레이리스트 수정 |
| `playlist-modify-private` | 비공개 플레이리스트 수정 |

#### 사용자 프로필 및 팔로우

| Scope | 설명 |
|-------|------|
| `user-read-private` | 사용자 프로필 읽기 (국가, 구독 상태 등) |
| `user-read-email` | 사용자 이메일 읽기 |
| `user-follow-read` | 팔로우 목록 읽기 |
| `user-follow-modify` | 팔로우/언팔로우 |
| `user-top-read` | 사용자 탑 아티스트/트랙 읽기 |

---

## 사용 가능한 엔드포인트

> 아래는 2026년 2월 변경 이후 **Development Mode에서 사용 가능한** 주요 엔드포인트이다.  
> Extended Quota Mode는 기존 전체 엔드포인트에 접근 가능하다.

### 🔍 Search

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/search` | 카탈로그 검색 (albums, artists, playlists, tracks, shows, episodes, audiobooks) |

**변경사항:**
- `limit` 파라미터: max **10** (이전 50), default **5** (이전 20)
- `offset` 파라미터: 기존과 동일

**요청 예시:**
```http
GET https://api.spotify.com/v1/search?q=BTS&type=track&limit=10
Authorization: Bearer {ACCESS_TOKEN}
```

### 🎵 Tracks

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/tracks/{id}` | 단일 트랙 정보 조회 |
| `GET` | `/tracks` | 여러 트랙 일괄 조회 (`?ids=id1,id2,...`) |
| `GET` | `/me/tracks` | 사용자 저장 트랙 목록 |
| `GET` | `/me/player/recently-played` | 최근 재생 트랙 |

### 💿 Albums

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/albums/{id}` | 단일 앨범 정보 조회 |
| `GET` | `/albums/{id}/tracks` | 앨범 트랙 목록 |

### 🎤 Artists

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/artists/{id}` | 단일 아티스트 정보 조회 |
| `GET` | `/artists/{id}/albums` | 아티스트 앨범 목록 |

### 📚 Library (신규 통합 엔드포인트)

기존의 타입별 개별 엔드포인트를 대체하는 **통합 라이브러리 엔드포인트**이다.

| Method | Endpoint | 설명 |
|--------|----------|------|
| `PUT` | `/me/library` | 아이템 저장 (Spotify URI 사용) |
| `DELETE` | `/me/library` | 아이템 제거 (Spotify URI 사용) |
| `GET` | `/me/library/contains` | 아이템 저장 여부 확인 |

**요청 예시 — 아이템 저장:**
```http
PUT https://api.spotify.com/v1/me/library
Authorization: Bearer {ACCESS_TOKEN}
Content-Type: application/json

{
  "uris": [
    "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
    "spotify:album:1DFixLWuPkv3KT3TnV35m3"
  ]
}
```

**요청 예시 — 저장 여부 확인:**
```http
GET https://api.spotify.com/v1/me/library/contains?uris=spotify:track:4iV5W9uYEdYUVa79Axb7Rh
Authorization: Bearer {ACCESS_TOKEN}
```

### 📋 Playlists

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/me/playlists` | 현재 사용자 플레이리스트 목록 |
| `GET` | `/playlists/{id}` | 플레이리스트 상세 정보 |
| `GET` | `/playlists/{id}/items` | 플레이리스트 아이템 목록 **(변경)** |
| `POST` | `/playlists/{id}/items` | 플레이리스트에 아이템 추가 **(변경)** |
| `POST` | `/me/playlists` | 새 플레이리스트 생성 **(변경)** |

**변경사항:**
- `GET /playlists/{id}/tracks` → `GET /playlists/{id}/items`
- `POST /playlists/{id}/tracks` → `POST /playlists/{id}/items`
- `POST /users/{user_id}/playlists` → `POST /me/playlists`

### ▶️ Player

| Method | Endpoint | 설명 | Scope |
|--------|----------|------|-------|
| `GET` | `/me/player` | 현재 재생 상태 | `user-read-playback-state` |
| `GET` | `/me/player/devices` | 활성 디바이스 목록 | `user-read-playback-state` |
| `PUT` | `/me/player/play` | 재생 시작/재개 | `user-modify-playback-state` |
| `PUT` | `/me/player/pause` | 일시정지 | `user-modify-playback-state` |
| `POST` | `/me/player/next` | 다음 트랙 | `user-modify-playback-state` |
| `POST` | `/me/player/previous` | 이전 트랙 | `user-modify-playback-state` |
| `POST` | `/me/player/queue` | 큐에 아이템 추가 | `user-modify-playback-state` |
| `PUT` | `/me/player/shuffle` | 셔플 모드 토글 | `user-modify-playback-state` |
| `PUT` | `/me/player/repeat` | 반복 모드 설정 | `user-modify-playback-state` |
| `PUT` | `/me/player/volume` | 볼륨 조절 | `user-modify-playback-state` |

### 👤 User Profile

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/me` | 현재 사용자 프로필 |
| `GET` | `/me/top/artists` | 사용자 탑 아티스트 |
| `GET` | `/me/top/tracks` | 사용자 탑 트랙 |

---

## 제거된 엔드포인트

> ⚠️ 아래 엔드포인트는 2026년 2월 업데이트로 **제거 또는 Development Mode에서 접근 불가**가 되었다.

### 라이브러리 (통합 엔드포인트로 대체)

| 제거된 엔드포인트 | 대체 |
|-------------------|------|
| `PUT /me/tracks` | `PUT /me/library` (URI: `spotify:track:...`) |
| `DELETE /me/tracks` | `DELETE /me/library` |
| `PUT /me/albums` | `PUT /me/library` (URI: `spotify:album:...`) |
| `DELETE /me/albums` | `DELETE /me/library` |
| `PUT /me/episodes` | `PUT /me/library` (URI: `spotify:episode:...`) |
| `DELETE /me/episodes` | `DELETE /me/library` |
| `PUT /me/shows` | `PUT /me/library` (URI: `spotify:show:...`) |
| `DELETE /me/shows` | `DELETE /me/library` |
| `PUT /me/audiobooks` | `PUT /me/library` (URI: `spotify:audiobook:...`) |
| `DELETE /me/audiobooks` | `DELETE /me/library` |
| `GET /me/tracks/contains` 등 | `GET /me/library/contains` |

### 카탈로그 / Browse

| 제거된 엔드포인트 | 대체 |
|-------------------|------|
| `GET /albums` (여러 앨범 조회) | 개별 `GET /albums/{id}` 사용 |
| `GET /artists` (여러 아티스트 조회) | 개별 `GET /artists/{id}` 사용 |
| `GET /audiobooks` | 대체 없음 |
| `GET /browse/categories` | 대체 없음 |
| `GET /browse/new-releases` | 대체 없음 |
| `GET /artists/{id}/top-tracks` | 대체 없음 |

### 사용자 / 소셜

| 제거된 엔드포인트 | 대체 |
|-------------------|------|
| `GET /users/{id}` | 대체 없음 (자기 프로필은 `GET /me` 사용) |
| `GET /users/{id}/playlists` | `GET /me/playlists` (자기 것만 가능) |
| `POST /users/{user_id}/playlists` | `POST /me/playlists` |
| `GET /me/following/contains` | 대체 없음 |
| `GET /playlists/{id}/followers/contains` | 대체 없음 |

### 플레이리스트 관리

| 제거된 엔드포인트 | 대체 |
|-------------------|------|
| `GET /playlists/{id}/tracks` | `GET /playlists/{id}/items` |
| `POST /playlists/{id}/tracks` | `POST /playlists/{id}/items` |
| `DELETE /playlists/{id}/tracks` | 대체 워크플로우 필요 |
| `PUT /playlists/{id}/tracks` | 대체 워크플로우 필요 |

### Audio Features / Analysis

| 제거된 엔드포인트 | 비고 |
|-------------------|------|
| `GET /audio-features/{id}` | 2024년 말 deprecation, Development Mode 접근 불가 |
| `GET /audio-features` (batch) | 동일 |
| `GET /audio-analysis/{id}` | 동일 |

**대안:**
- Extended Quota Mode에서는 기존 접근 유지 가능
- 서드파티 API (RapidAPI 등의 Audio Analysis API)
- 오픈소스 분석 도구: **Librosa** (Python), **MTG-Essentia**
- 자체 분석 DB 구축

---

## Rate Limiting

### 기본 정책

| 항목 | 설명 |
|------|------|
| 측정 방식 | **30초 롤링 윈도우** 기반 총 요청 수 |
| 초과 시 응답 | `429 Too Many Requests` |
| 재시도 | `Retry-After` 헤더 값(초) 만큼 대기 후 재시도 |

### 모드별 차이

| 모드 | Rate Limit |
|------|-----------|
| Development Mode | 더 엄격한 제한 (정확한 수치 비공개) |
| Extended Quota Mode | 더 높은 한도 (Dashboard에서 확인) |

### 429 에러 처리 예시

```javascript
async function spotifyRequest(url, options, maxRetries = 3) {
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    const response = await fetch(url, options);

    if (response.status === 429) {
      const retryAfter = parseInt(response.headers.get('Retry-After') || '1');
      console.warn(`Rate limited. Retrying after ${retryAfter}s...`);
      await new Promise(resolve => setTimeout(resolve, retryAfter * 1000));
      continue;
    }

    return response;
  }

  throw new Error('Max retries exceeded');
}
```

### Rate Limit 최소화 전략

1. **배치 엔드포인트 활용** — 개별 요청 대신 일괄 조회 사용
2. **캐싱 구현** — 변경이 적은 데이터는 캐시 적용
3. **Exponential Backoff** — 429 응답 시 지수적 대기
4. **요청 큐잉** — 동시 요청 수 제한

---

## 마이그레이션 가이드

### 1. 라이브러리 엔드포인트 마이그레이션

**Before (제거됨):**
```javascript
// 트랙 저장
await fetch('https://api.spotify.com/v1/me/tracks', {
  method: 'PUT',
  headers: { 'Authorization': `Bearer ${token}` },
  body: JSON.stringify({ ids: ['4iV5W9uYEdYUVa79Axb7Rh'] })
});
```

**After (신규):**
```javascript
// 통합 라이브러리 엔드포인트로 저장
await fetch('https://api.spotify.com/v1/me/library', {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    uris: ['spotify:track:4iV5W9uYEdYUVa79Axb7Rh']
  })
});
```

### 2. 플레이리스트 엔드포인트 마이그레이션

**Before (제거됨):**
```javascript
// 플레이리스트 트랙 조회
GET /playlists/{id}/tracks

// 플레이리스트에 트랙 추가
POST /playlists/{id}/tracks

// 사용자의 플레이리스트 생성
POST /users/{user_id}/playlists
```

**After (신규):**
```javascript
// 플레이리스트 아이템 조회
GET /playlists/{id}/items

// 플레이리스트에 아이템 추가
POST /playlists/{id}/items

// 내 플레이리스트 생성
POST /me/playlists
```

### 3. Implicit Grant → PKCE 마이그레이션

```javascript
// PKCE Flow 구현 예시
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

// 인증 URL 생성
const codeVerifier = generateCodeVerifier();
const codeChallenge = await generateCodeChallenge(codeVerifier);

const authUrl = new URL('https://accounts.spotify.com/authorize');
authUrl.searchParams.set('client_id', CLIENT_ID);
authUrl.searchParams.set('response_type', 'code');
authUrl.searchParams.set('redirect_uri', REDIRECT_URI);
authUrl.searchParams.set('scope', 'user-library-read user-read-playback-state');
authUrl.searchParams.set('code_challenge_method', 'S256');
authUrl.searchParams.set('code_challenge', codeChallenge);

// codeVerifier는 sessionStorage 등에 저장해두고 token 교환 시 사용
```

---

## 개발자 권장사항

### ✅ Do

- **PKCE Flow 사용** — Implicit Grant는 폐기됨
- **통합 라이브러리 엔드포인트 사용** — `/me/library` 활용
- **Spotify URI 형식 사용** — `spotify:{type}:{id}` 형식 준수
- **429 에러 핸들링** — `Retry-After` 헤더 기반 backoff 구현
- **배치 요청 활용** — 가능하면 개별 요청 대신 일괄 조회
- **캐싱 적용** — 카탈로그 데이터는 적극적으로 캐싱
- **Extended Quota Mode 신청** — 5명 이상 사용자가 필요하면 필수

### ❌ Don't

- **Development Mode로 상용 서비스 운영 금지** — 5명 제한
- **제거된 엔드포인트 사용 금지** — 즉시 마이그레이션 필요
- **과도한 스크래핑 패턴 금지** — rate limit 및 계정 제재 위험
- **Implicit Grant Flow 사용 금지** — 보안 취약, 폐기됨
- **Premium 구독 만료 방치 금지** — Development Mode 앱 즉시 작동 중지

### 프로젝트 적용 체크리스트

- [ ] Development Mode / Extended Quota Mode 확인
- [ ] 앱 소유자 Premium 구독 확인
- [ ] Implicit Grant → PKCE Flow 마이그레이션
- [ ] 개별 라이브러리 엔드포인트 → `/me/library` 마이그레이션
- [ ] `/playlists/{id}/tracks` → `/playlists/{id}/items` 마이그레이션
- [ ] `/users/{id}/playlists` → `/me/playlists` 마이그레이션
- [ ] Search `limit` 파라미터 조정 (max 10)
- [ ] 429 에러 핸들링 구현
- [ ] Audio Features 사용 시 대안 검토

---

## 참고 링크

| 리소스 | URL |
|--------|-----|
| 공식 API Reference | https://developer.spotify.com/documentation/web-api/reference |
| 2026년 2월 Changelog | https://developer.spotify.com/documentation/web-api/references/changes/february-2026 |
| Migration Guide | https://developer.spotify.com/documentation/web-api/guides/migration-guide |
| Developer Dashboard | https://developer.spotify.com/dashboard |
| OAuth Scopes | https://developer.spotify.com/documentation/web-api/concepts/scopes |
| PKCE Flow Tutorial | https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow |
| Rate Limits | https://developer.spotify.com/documentation/web-api/concepts/rate-limits |
