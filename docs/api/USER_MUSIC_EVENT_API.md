# User Music Event API

작성일: `2026-05-11`

## 목적

`User Music Event API`는 추천 모델 학습에 사용할 사용자 행동 로그를 적재합니다.

이 API는 PMS, EMS, GMS, 검색 상세, 플레이어 같은 화면별 기능에 묶이지 않고, 사용자가 실제로 음악을 듣고 반응한 사건을 공통 스키마로 저장합니다.

## 경로

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/recommendations/events` | 사용자 음악 행동 이벤트 적재 |
| `GET` | `/api/v1/recommendations/datasets/users/{userId}/sequence` | 사용자 이벤트와 추천 스냅샷을 모델 학습 sequence로 export |

## 요청

```json
{
  "user_id": "user-001",
  "event_type": "play_started",
  "source_space": "player",
  "source_platform": "spotify",
  "playback_platform_id": "tidal",
  "item_id": "ems-track-100",
  "item_kind": "track",
  "track_id": "ems-track-100",
  "playlist_id": "ems-playlist-001",
  "external_track_id": "spotify-track-001",
  "platform_uri": "spotify:track:spotify-track-001",
  "title": "Midnight Receiver",
  "artist_name": "Neon Bloom",
  "album_title": "Signal Bloom",
  "isrc": "USRC17607839",
  "duration_ms": 180000,
  "position_ms": 0,
  "play_ratio": 0,
  "recommendation_id": null,
  "metadata_confidence": 0.8,
  "occurred_at": "2026-05-11T00:00:00Z"
}
```

필수 필드:

- `user_id`
- `event_type`

`source_space`가 비어 있으면 서버는 `player`로 저장합니다.

## 이벤트 타입

초기 지원 이벤트:

- `play_started`
- `play_paused`
- `play_resumed`
- `play_completed`
- `skip_next`
- `skip_previous`
- `replay`
- `track_saved`
- `added_to_playlist`
- `recommendation_liked`
- `recommendation_rejected`
- `ignored_recommendation`
- `stopped_midway`

## 응답

```json
{
  "service": "user-music-event",
  "status": "recorded",
  "processed_at": "2026-05-11T00:00:00Z",
  "event": {
    "event_id": 1,
    "user_id": "user-001",
    "event_type": "play_started",
    "event_weight": 0,
    "source_space": "player",
    "source_platform": "spotify",
    "playback_platform_id": "tidal",
    "item_id": "ems-track-100",
    "item_kind": "track",
    "track_id": "ems-track-100",
    "playlist_id": "ems-playlist-001",
    "external_track_id": "spotify-track-001",
    "platform_uri": "spotify:track:spotify-track-001",
    "title": "Midnight Receiver",
    "artist_name": "Neon Bloom",
    "album_title": "Signal Bloom",
    "isrc": "USRC17607839",
    "duration_ms": 180000,
    "position_ms": 0,
    "play_ratio": 0,
    "recommendation_id": null,
    "metadata_confidence": 0.8,
    "occurred_at": "2026-05-11T00:00:00Z",
    "received_at": "2026-05-11T00:00:00Z"
  },
  "next_step_message": "Event is now available as a recommendation learning signal."
}
```

## 현재 구현 메모

- DB migration: `V23__create_user_music_event.sql`
- 추천 스냅샷 migration: `V24__create_recommendation_snapshot.sql`
- 저장소: local profile은 in-memory, non-local profile은 JPA/PostgreSQL
- 웹 플레이어는 현재 `play_started`, `play_paused`, `play_resumed`, `play_completed`, `skip_next`, `skip_previous`를 적재합니다.
- PMS 개인 플레이리스트 저장은 `added_to_playlist`를 적재합니다.
- GMS feedback은 `like -> recommendation_liked`, `dislike -> recommendation_rejected`, `save -> track_saved`, `skip -> ignored_recommendation`으로 함께 적재합니다.
- GMS preview 결과는 `recommendation_snapshot`에 저장되며, playlist-level `coherence_score`, `diversity_score`, `redundancy_penalty`가 함께 남습니다.
- dataset exporter는 `user_music_event`와 `recommendation_snapshot`을 시간순 `sequence`로 합쳐 AI service 학습/검증 경계에 제공합니다.
- 적재 실패는 재생을 막지 않습니다. 추천 학습 로그는 제품 사용 흐름보다 후순위입니다.

## 다음 연결 지점

- AI service dataset import harness 추가
- SASRec MVP 학습 스크립트 추가
