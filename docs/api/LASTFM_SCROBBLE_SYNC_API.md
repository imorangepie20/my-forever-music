# Last.fm Scrobble Sync API

작성일: `2026-05-04`

## 목적

저장된 `Last.fm profile username`을 기준으로 최근 scrobble을 계정 단위로 적재하고, 이후 `EMS / GMS`가 재사용할 수 있는 장기 청취 snapshot을 만드는 API입니다.

이 API는 public preview와 다릅니다. preview는 브라우저에서 바로 보는 임시 조회이고, scrobble sync는 사용자 계정에 붙는 저장 단계입니다.

## 엔드포인트

- `GET /api/v1/platforms/lastfm/scrobbles/bootstrap`
- `POST /api/v1/platforms/lastfm/scrobbles/sync`

## 요청

### Bootstrap Query

```text
GET /api/v1/platforms/lastfm/scrobbles/bootstrap?user_id=user-001
```

### Sync Body

```json
{
  "user_id": "user-001",
  "limit": 40
}
```

## 응답

### Bootstrap 예시

```json
{
  "service": "api",
  "status": "ok",
  "generated_at": "2026-05-04T04:10:00Z",
  "user": {
    "user_id": "user-001",
    "last_fm_username": "mibeen",
    "last_fm_connected_at": "2026-05-04T03:40:00Z"
  },
  "summary": {
    "stored_scrobble_count": 24,
    "last_synced_at": "2026-05-04T04:05:00Z",
    "returned_scrobble_count": 10,
    "next_step_message": "Recent Last.fm scrobbles are stored and ready for future EMS/GMS modeling."
  },
  "recent_scrobbles": [
    {
      "track_name": "Days of Thunder",
      "artist_name": "The Midnight",
      "album_name": "Days of Thunder",
      "track_url": "https://www.last.fm/music/The+Midnight/_/Days+of+Thunder",
      "image_url": null,
      "played_at": "2026-05-03T20:00:00Z",
      "loved": true,
      "synced_at": "2026-05-04T04:05:00Z"
    }
  ]
}
```

### Sync 예시

```json
{
  "service": "api",
  "status": "synced",
  "processed_at": "2026-05-04T04:05:00Z",
  "sync": {
    "user_id": "user-001",
    "last_fm_username": "mibeen",
    "fetched_track_count": 40,
    "inserted_scrobble_count": 24,
    "duplicate_scrobble_count": 12,
    "skipped_now_playing_count": 1,
    "stored_scrobble_count": 24,
    "last_synced_at": "2026-05-04T04:05:00Z"
  },
  "recent_scrobbles": [
    {
      "track_name": "Days of Thunder",
      "artist_name": "The Midnight",
      "album_name": "Days of Thunder",
      "track_url": "https://www.last.fm/music/The+Midnight/_/Days+of+Thunder",
      "image_url": null,
      "played_at": "2026-05-03T20:00:00Z",
      "loved": true,
      "synced_at": "2026-05-04T04:05:00Z"
    }
  ],
  "notes": [
    "Stored scrobbles are deduplicated by username, played_at, artist, and track name."
  ]
}
```

## 현재 구현 메모

- 이 API는 먼저 계정에 저장된 `last_fm_username`이 있어야 동작합니다.
- sync는 `Last.fm recent tracks`를 읽되, `now playing` 행은 안정적인 timestamp가 없어서 저장에서 제외합니다.
- 적재 키는 `user_id + last_fm_username + played_at + artist_name + track_name` 입니다.
- `local` 프로필에서는 메모리 저장소를 사용하고, `!local` 프로필에서는 `lastfm_scrobble` 테이블에 영속 저장합니다.
- `EMS workspace analysis`와 `GMS recommendation preview`는 이제 저장된 scrobble snapshot이 있으면 그 artist recurrence를 먼저 사용하고, 비어 있으면 live `Last.fm top artists` 조회로 fallback 합니다.

## 다음 연결 지점

1. 수동 sync를 주기 배치 sync로 확장
2. Last.fm scrobble을 Spotify 매칭 및 오디오 특성 파이프라인과 연결
3. 시계열 청취 변화와 repeat affinity를 사용자 모델에 직접 반영
