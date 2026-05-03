# API Docs Index

작성일: `2026-05-02`

이 디렉토리는 `my-forever-music`의 API 계약 문서와 API 저장 정책 문서를 모아둔 곳입니다.

가장 먼저 볼 문서는 [PROJECT_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_GUIDE.md) 와 [PROJECT_KEY_SERVICE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_KEY_SERVICE.md) 이고, 그 다음 API 작업자는 이 문서를 진입점으로 사용합니다.

## 읽는 순서

1. [PLATFORM_CATALOG_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PLATFORM_CATALOG_API.md)
2. [AUTH_REGISTER_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/AUTH_REGISTER_API.md)
3. [AUTH_LOGIN_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/AUTH_LOGIN_API.md)
4. [PLATFORM_CONNECTION_ONBOARDING_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PLATFORM_CONNECTION_ONBOARDING_API.md)
5. [LASTFM_SIGNAL_PREVIEW_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/LASTFM_SIGNAL_PREVIEW_API.md)
6. [LASTFM_PROFILE_CONNECTION_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/LASTFM_PROFILE_CONNECTION_API.md)
7. [LASTFM_SCROBBLE_SYNC_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/LASTFM_SCROBBLE_SYNC_API.md)
8. [PLATFORM_OAUTH_SANDBOX_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PLATFORM_OAUTH_SANDBOX_API.md)
9. [PMS_PLAYLIST_IMPORT_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_PLAYLIST_IMPORT_API.md)
10. [PMS_WORKSPACE_BOOTSTRAP_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_WORKSPACE_BOOTSTRAP_API.md)
11. [EMS_WORKSPACE_ANALYSIS_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/EMS_WORKSPACE_ANALYSIS_API.md)
12. [GMS_RECOMMENDATION_PREVIEW_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/GMS_RECOMMENDATION_PREVIEW_API.md)
13. [AI_RECOMMENDATION_PREVIEW.md](/Users/woosungjo/music-space/my-forever-music/docs/api/AI_RECOMMENDATION_PREVIEW.md)
14. [PMS_TRACK_AUDIO_FEATURE_STORAGE.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_TRACK_AUDIO_FEATURE_STORAGE.md)

## 문서 분류

### 1. 공개 API 계약

웹앱, 데스크탑 앱, 혹은 외부 클라이언트가 `services/api`를 호출할 때 기준이 되는 문서입니다.

- [PLATFORM_CATALOG_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PLATFORM_CATALOG_API.md)
- [AUTH_REGISTER_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/AUTH_REGISTER_API.md)
- [AUTH_LOGIN_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/AUTH_LOGIN_API.md)
- [PLATFORM_CONNECTION_ONBOARDING_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PLATFORM_CONNECTION_ONBOARDING_API.md)
- [LASTFM_SIGNAL_PREVIEW_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/LASTFM_SIGNAL_PREVIEW_API.md)
- [LASTFM_PROFILE_CONNECTION_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/LASTFM_PROFILE_CONNECTION_API.md)
- [LASTFM_SCROBBLE_SYNC_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/LASTFM_SCROBBLE_SYNC_API.md)
- [PLATFORM_OAUTH_SANDBOX_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PLATFORM_OAUTH_SANDBOX_API.md)
- [PMS_PLAYLIST_IMPORT_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_PLAYLIST_IMPORT_API.md)
- [PMS_WORKSPACE_BOOTSTRAP_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_WORKSPACE_BOOTSTRAP_API.md)
- [EMS_WORKSPACE_ANALYSIS_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/EMS_WORKSPACE_ANALYSIS_API.md)
- [GMS_RECOMMENDATION_PREVIEW_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/GMS_RECOMMENDATION_PREVIEW_API.md)

### 2. 내부 서비스 계약

`services/api`와 `services/ai` 사이의 내부 호출 계약입니다.

- [AI_RECOMMENDATION_PREVIEW.md](/Users/woosungjo/music-space/my-forever-music/docs/api/AI_RECOMMENDATION_PREVIEW.md)

### 3. 저장 정책 / 구현 기준

엔드포인트 계약은 아니지만, API 구현과 DB 적재 방식에 직접 영향을 주는 기준 문서입니다.

- [PMS_TRACK_AUDIO_FEATURE_STORAGE.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_TRACK_AUDIO_FEATURE_STORAGE.md)

## 현재 엔드포인트 맵

| 문서 | 메서드 | 경로 | 소유 서비스 | 성격 |
| --- | --- | --- | --- | --- |
| Platform Catalog API | `GET` | `/api/v1/platforms/catalog` | `services/api` | 공개 API |
| Auth Register API | `POST` | `/api/v1/auth/register` | `services/api` | 공개 API |
| Auth Login API | `POST` | `/api/v1/auth/login` | `services/api` | 공개 API |
| Platform Connection Onboarding API | `GET` | `/api/v1/platforms/connections/bootstrap` | `services/api` | 공개 API |
| Platform Connection Onboarding API | `POST` | `/api/v1/platforms/connections/connect` | `services/api` | 공개 API |
| Platform Connection Onboarding API | `POST` | `/api/v1/platforms/connections/disconnect` | `services/api` | 공개 API |
| Last.fm Signal Preview API | `GET` | `/api/v1/platforms/lastfm/preview` | `services/api` | 공개 API |
| Last.fm Profile Connection API | `POST` | `/api/v1/platforms/lastfm/profile` | `services/api` | 공개 API |
| Last.fm Scrobble Sync API | `GET` | `/api/v1/platforms/lastfm/scrobbles/bootstrap` | `services/api` | 공개 API |
| Last.fm Scrobble Sync API | `POST` | `/api/v1/platforms/lastfm/scrobbles/sync` | `services/api` | 공개 API |
| Platform OAuth Sandbox API | `POST` | `/api/v1/platforms/oauth/start` | `services/api` | 공개 API |
| Platform OAuth Sandbox API | `POST` | `/api/v1/platforms/oauth/complete` | `services/api` | 공개 API |
| PMS Playlist Import API | `GET` | `/api/v1/pms/import/bootstrap` | `services/api` | 공개 API |
| PMS Playlist Import API | `POST` | `/api/v1/pms/import/playlists` | `services/api` | 공개 API |
| PMS Workspace Bootstrap API | `GET` | `/api/v1/pms/workspace/bootstrap` | `services/api` | 공개 API |
| EMS Workspace Analysis API | `POST` | `/api/v1/ems/workspace/analysis` | `services/api` | 공개 API |
| GMS Recommendation Preview API | `POST` | `/api/v1/gms/recommendations/preview` | `services/api` | 공개 API |
| AI Recommendation Preview API | `POST` | `/v1/recommendations/preview` | `services/ai` | 내부 계약 |

## 정리 원칙

- 새 공개 API를 추가하면 이 문서의 `현재 엔드포인트 맵`부터 갱신합니다.
- `services/api`가 외부에 노출하는 경로와 `services/ai` 내부 계약은 문서를 분리합니다.
- 저장 구조나 import 정책처럼 엔드포인트 바깥의 기준은 `저장 정책 / 구현 기준` 문서로 둡니다.
- 엔드포인트 문서는 가능하면 `목적 -> 경로 -> 요청/응답 -> 현재 구현 메모 -> 다음 연결 지점` 순서를 유지합니다.

## 관련 문서

- [PROJECT_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_GUIDE.md)
- [PROJECT_KEY_SERVICE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_KEY_SERVICE.md)
- [services/api/README.md](/Users/woosungjo/music-space/my-forever-music/services/api/README.md)
- [services/ai/README.md](/Users/woosungjo/music-space/my-forever-music/services/ai/README.md)
