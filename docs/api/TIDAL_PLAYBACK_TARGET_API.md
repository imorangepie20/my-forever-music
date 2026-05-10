# TIDAL Playback Target API

작성일: `2026-05-10`

이 문서는 `services/api`가 TIDAL 재생을 시작하기 전에 트랙 metadata를 실제 TIDAL 재생 target으로 해석하는 계약입니다.

## 목적

PMS 또는 EMS에 저장된 트랙이 Spotify 등 다른 플랫폼에서 온 경우에도, 사용자의 현재 재생 플랫폼이 `tidal`이면 재생 직전에 TIDAL에서 해당 트랙을 검색해 playable TIDAL track id를 찾습니다.

이 API는 검색 결과를 EMS/PMS 테이블에 저장하지 않습니다. 검색 결과는 현재 playback queue 안에서만 임시 playback target으로 사용합니다.

## Endpoint

| Method | Path | Owner |
| --- | --- | --- |
| `POST` | `/api/v1/platforms/playback/tidal/resolve-track` | `services/api` |
| `GET` | `/api/v1/platforms/playback/tidal/tracks/{track_id}/stream` | `services/api` |

## Resolve Request

```json
{
  "user_id": "user-001",
  "title": "Midnight Signal",
  "artist_name": "Neon Bloom",
  "source_platform": "spotify",
  "external_track_id": "spotify-track-001",
  "platform_uri": "spotify:track:spotify-track-001",
  "spotify_track_id": "spotify-track-001",
  "isrc": "USRC17607839",
  "duration_ms": 218000
}
```

필수:

- `user_id`
- `title`
- `artist_name`

선택 metadata:

- `source_platform`
- `external_track_id`
- `platform_uri`
- `spotify_track_id`
- `isrc`
- `duration_ms`

## Resolve Response

```json
{
  "service": "api",
  "status": "ok",
  "generated_at": "2026-05-10T12:00:00Z",
  "user_id": "user-001",
  "source_platform": "spotify",
  "source_track_id": "spotify-track-001",
  "tidal_track_id": "tidal-track-001",
  "tidal_uri": "tidal:track:tidal-track-001",
  "title": "Midnight Signal",
  "artist_name": "Neon Bloom",
  "album_title": "Night Index",
  "album_image_url": null,
  "platform_external_url": "https://tidal.com/browse/track/tidal-track-001",
  "preview_url": null,
  "isrc": "USRC17607839",
  "duration_ms": 218000,
  "match_reason": "isrc",
  "match_score": 100
}
```

## Matching Policy

`TidalPlaybackTargetResolverService`는 저장된 TIDAL credential을 먼저 확인하고, credential이 없거나 refresh 불가 상태면 provider 오류를 그대로 반환합니다.

후보 검색과 매칭 기준:

- TIDAL 검색 query는 `title + artist_name`입니다.
- ISRC가 일치하면 `match_reason=isrc`, `match_score=100`입니다.
- ISRC가 없거나 맞지 않으면 title, artist, duration metadata를 점수화합니다.
- metadata match는 score `70` 이상만 playable target으로 인정합니다.
- ISRC match와 metadata match가 동점이면 ISRC match를 우선합니다.
- 매칭되는 후보가 없으면 성공처럼 처리하지 않고 `ApiResourceNotFoundException`을 던집니다.

## Stream Contract

resolve-track 응답의 `tidal_track_id`는 `GET /api/v1/platforms/playback/tidal/tracks/{track_id}/stream`에 전달됩니다.

Stream endpoint는 TIDAL playbackinfo 요청에서 `assetpresentation=FULL`을 요구합니다.

- TIDAL이 `FULL`이 아닌 preview manifest를 반환하면 재생 성공으로 취급하지 않습니다.
- TIDAL credential, scope, country, subscription, provider 오류는 mock stream, preview URL, Spotify fallback으로 대체하지 않습니다.
- browser player가 재생할 수 없는 DASH manifest는 현재 직접 browser player에서 실패로 표시합니다.

## Frontend Usage

`apps/web`의 `resolveTidalPlayableItem`은 기존 item에 TIDAL id가 없을 때만 이 API를 호출합니다.

`playQueue`는 TIDAL 재생을 시작할 때:

1. 기존 player 상태를 초기화합니다.
2. 새 pending player를 띄웁니다.
3. TIDAL playable target을 트랙별로 resolve 합니다.
4. 선택한 트랙을 stream endpoint로 재생합니다.

EMS/PMS 화면은 playlist 자체를 직접 재생하지 않고, DB에 저장된 playlist detail tracks를 읽은 뒤 track queue로 넘깁니다.

## Related Code

- `services/api/src/main/java/io/myforevermusic/api/modules/platform/application/TidalPlaybackTargetResolverService.java`
- `services/api/src/main/java/io/myforevermusic/api/modules/platform/presentation/TidalPlaybackTargetController.java`
- `services/api/src/main/java/io/myforevermusic/api/modules/platform/presentation/TidalPlaybackStreamController.java`
- `apps/web/src/lib/tidalStreamPlayback.ts`
- `apps/web/src/contexts/PlaybackContext.tsx`
