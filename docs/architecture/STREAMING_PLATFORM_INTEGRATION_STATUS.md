# Streaming Platform Integration Status

작성일: `2026-05-05`
최종 업데이트: `2026-05-06`

이 문서는 `my-forever-music`의 스트리밍 플랫폼 연동 현재 상태를 설명합니다.

## 권장사항 (2026-05-05 기준)

### 현재 집중: Spotify & TIDAL

**PMS playlist import를 위한 권장 플랫폼:**

1. **Spotify** ✅ - 1차 기준, 완전히 구현됨
2. **TIDAL** 🟡 - 2차 우선, 현재 검증 단계

### 보류: YouTube Music & Apple Music

| 플랫폼 | 보류 사유 |
|--------|-----------|
| **YouTube Music** | 공식 API가 없음. YouTube Data API v3는 Music 전용 기능 제공하지 않음. PMS import 불가. |
| **Apple Music** | Apple Developer Program membership ($99/년) 필요. 준비 시까지 보류. |

---

## 통합 개요

| 플랫폼 | 상태 | OAuth | Playlist Provider | Audio Features | PMS Import |
|--------|------|-------|------------------|----------------|------------|
| Spotify | ✅ 완료 | OAuth 2.0 + PKCE | ✅ 구현됨 | ReccoBeats lookup | ✅ 활성 |
| TIDAL | 🟡 검증중 | OAuth 2.1 + PKCE | ✅ 구현됨 | ReccoBeats ISRC match | 🟡 테스트 |
| YouTube Music | ❌ 보류 | - | 플레이스홀더 | - | ❌ 비활성 |
| Apple Music | ❌ 보류 | - | 플레이스홀더 | - | ❌ 비활성 |
| Last.fm | ✅ 완료 | API Key | ✅ Signal Source | - | ❌ 해당없음 |

---

## Spotify

### 구현 완료 상태

- **인증**: OAuth 2.0 + PKCE
- **Playlist Provider**: `SpotifyPlatformPlaylistProvider`
- **Web API Client**: `SpotifyWebApiClient`
- **Token Refresh**: `SpotifyTokenRefreshClient`
- **Audio Features**: ReccoBeats 조회형 API 기반

### 지원 기능

- 사용자 플레이리스트 목록 조회
- 플레이리스트 아이템 (트랙) 로드
- 오디오 특성 snapshot 보강과 저장
- Access token 자동 갱신
- Refresh 실패 시 `reconnect_required` 상태 반환

### API 엔드포인트

| 엔드포인트 | 설명 |
|-----------|------|
| `GET /me/playlists` | 사용자 플레이리스트 목록 |
| `GET /playlists/{id}/tracks` | 플레이리스트 트랙 목록 |
| `POST /api/token` | Token 갱신 |

---

## TIDAL

### 구현 완료 상태

- **인증**: OAuth 2.1 + PKCE (`TidalAuthorizationCodeExchangeClient`, `TidalTokenRefreshClient`)
- **Playlist Provider**: `TidalPlatformPlaylistProvider` ✅ 신규 구현
- **Web API Client**: `TidalWebApiClient` ✅ 신규 구현
- **Audio Features**: ReccoBeats ISRC match 구현 완료

### 지원 기능

- 사용자 플레이리스트 목록 조회 (`/userCollectionPlaylists`)
- 플레이리스트 아이템 로드 (`/playlists/{id}?include=items,items.artists,items.albums`)
- TIDAL track `ISRC`, artist, album 메타데이터 파싱
- ReccoBeats 후보 매칭 기반 오디오 특성 보강
- Access token 자동 갱신

### API 규격

- **Base URL**: `https://openapi.tidal.com/v2`
- **응답 형식**: JSON:API (`application/vnd.api+json`)
- **필수 파라미터**: `countryCode`

### 향후 작업

1. 실제 TIDAL 계정으로 end-to-end import 검증
2. 대용량 플레이리스트 처리 테스트
3. 에러 복구 정책 고도화
4. provider-neutral schema migration 설계

---

## YouTube Music

### 현재 상태: 플레이스홀더

`YouTubeMusicPlatformPlaylistProvider`가 구현되었으나 실제 API 연동은 보류되었습니다.

### 제약 사항

- YouTube Music은 공개 API를 제공하지 않음
- YouTube Data API v3는 일반 YouTube용 (Music 특화 아님)
- 서드파티 라이브러리는 TOS 위반 가능성

### 대안 접근법

1. **YouTube Data API v3 활용**: EMS 신호源로 사용
   - 사용자 좋아요 영상
   - 재생 기록
   - 구독 채널

2. **브라우저 확장**: 사용자 직접 내보내기

3. **공식 API 대기**: YouTube가 Music专用 API를 release할 때까지

---

## Apple Music

### 현재 상태: 플레이스홀더

`AppleMusicPlatformPlaylistProvider`가 구현되었으나 실제 API 연동은 보류되었습니다.

### 선행 조건

1. Apple Developer Program 회원가입 ($99/년)
2. App ID 생성 + MusicKit capability
3. MusicKit private key 생성
4. JWT 인증 구성

### API 옵션

- **MusicKit JS**: 클라이언트 사이드, 사용자 상호작용 필요
- **Apple Music API**: 서버 사이드, developer token 필요

### 주요 엔드포인트 (구현 시)

```
GET /v1/catalog/{storefront}/users/{user id}/playlists
GET /v1/catalog/{storefront}/playlists/{id}
GET /v1/catalog/{storefront}/playlists/{id}/tracks
```

---

## Last.fm

### 구현 완료 상태

- **연동 유형**: Signal Source (PMS import 대상 아님)
- **API Client**: `LastFmWebApiClient`
- **서비스**:
  - `LastFmSignalPreviewService` - 공개 프로필 preview
  - `LastFmProfileConnectionService` - 사용자명 저장
  - `LastFmScrobbleSyncService` - scrobble 동기화

### 지원 기능

- 공개 사용자명 기반 recent scrobble 조회
- Top artist, top track 추출
- EMS seed artist로 활용
- GMS 추천 시 사용자 취향 반영

---

## 공통 인터페이스

### PlatformPlaylistProvider

```java
public interface PlatformPlaylistProvider {
    boolean supports(String platformId, PlatformAccountCredential credential);
    List<ImportCandidatePlaylist> listImportablePlaylists(...);
    List<ImportCandidatePlaylist> loadPlaylistsForImport(...);
}
```

### 구현체

- `SpotifyPlatformPlaylistProvider`
- `TidalPlatformPlaylistProvider`
- `YouTubeMusicPlatformPlaylistProvider` (placeholder)
- `AppleMusicPlatformPlaylistProvider` (placeholder)

### 자동 등록

Spring의 `@Component` 스캔으로 `PlatformPlaylistProviderRegistry`에 자동 등록됩니다.

---

## 다음 단계

### 1. TIDAL 검증 (우선)

- [ ] 실제 TIDAL 계정으로 플레이리스트 import 테스트
- [x] ReccoBeats 기반 TIDAL 오디오 특성 backfill 경로 구현
- [ ] 대용량 플레이리스트 처리 확인
- [ ] 에러 시나리오 테스트

### 2. YouTube Music 대안

- [ ] YouTube Data API v3로 EMS signal source 구현
- [ ] 사용자 좋아요/재생 기록 수집
- [ ] 음악 관련 콘텐츠 필터링

### 3. Apple Music 준비

- [ ] Apple Developer Program 가입
- [ ] MusicKit 자격 확보
- [ ] JWT 인증 구현
- [ ] 플레이스홀더를 실제 구현으로 교체

---

## 관련 문서

- [Spotify API Reference](/Users/woosungjo/music-space/my-forever-music/docs/streaming-platforms-api/spotify.md)
- [TIDAL API Reference](/Users/woosungjo/music-space/my-forever-music/docs/streaming-platforms-api/tidal.md)
- [PROJECT_KEY_SERVICE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_KEY_SERVICE.md)
- [REAL_IMPLEMENTATION_POLICY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/REAL_IMPLEMENTATION_POLICY.md)
