# PMS Personal Playlist API

작성일: `2026-05-05`

이 문서는 사용자가 PMS 안에서 직접 소유하는 playlist를 만들고, PMS library track을 저장하는 공개 API 계약입니다.

## 목적

- 가져온 플랫폼 playlist를 넘어서 사용자 소유 PMS playlist를 만들 수 있게 하기
- GMS 추천 후보의 `Save` 행동을 실제 PMS playlist 저장으로 연결하기
- 특정 스트리밍 플랫폼 계정에 묶이지 않는 개인 playlist 저장소를 준비하기

## 엔드포인트

- `GET /api/v1/pms/personal-playlists/bootstrap`
- `POST /api/v1/pms/personal-playlists`
- `POST /api/v1/pms/personal-playlists/tracks`

## Bootstrap 요청

```text
GET /api/v1/pms/personal-playlists/bootstrap?user_id=user-{uuid}
```

## Playlist 생성 요청

```json
{
  "user_id": "user-{uuid}",
  "title": "My Forever Finds",
  "description": "Songs saved inside my PMS library."
}
```

## Track 저장 요청

`target_playlist_id`를 생략하면 기본 `Saved GMS Recommendations` playlist를 만들거나 재사용합니다.

```json
{
  "user_id": "user-{uuid}",
  "target_playlist_title": "Saved GMS Recommendations",
  "track_id": "pms-track-spotify-{spotify_track_id}",
  "source_context": "gms-preview"
}
```

특정 개인 playlist에 저장할 때:

```json
{
  "user_id": "user-{uuid}",
  "target_playlist_id": "personal-my-forever-finds-1234abcd",
  "track_id": "pms-track-spotify-{spotify_track_id}",
  "source_context": "pms"
}
```

## 응답

```json
{
  "service": "pms-personal-playlists",
  "status": "saved",
  "processed_at": "2026-05-05T00:00:00Z",
  "playlist": {
    "playlist_id": "personal-saved-gms-recommendations",
    "title": "Saved GMS Recommendations",
    "description": "Tracks saved from GMS recommendation candidates.",
    "track_count": 1,
    "created_at": "2026-05-05T00:00:00Z",
    "updated_at": "2026-05-05T00:00:00Z",
    "tracks": [
      {
        "track_id": "pms-track-spotify-{spotify_track_id}",
        "title": "Track Title",
        "artist_name": "Artist",
        "source_platform": "spotify",
        "album_title": "Album",
        "album_image_url": "https://...",
        "platform_external_url": "https://open.spotify.com/track/...",
        "platform_uri": "spotify:track:...",
        "preview_url": null,
        "spotify_track_id": "{spotify_track_id}",
        "audio_feature_track_id": "{audio_feature_track_id}",
        "duration_ms": 218000,
        "sort_order": 1,
        "source_context": "gms-preview",
        "added_at": "2026-05-05T00:00:00Z"
      }
    ]
  },
  "next_step_message": "Track is now saved into a PMS personal playlist."
}
```

`audio_feature_track_id`는 개인 플레이리스트 트랙의 provider-neutral 권장 필드입니다.
`spotify_track_id`는 기존 클라이언트 호환을 위해 같은 값으로 유지합니다.

## 현재 구현 메모

- DB 활성 프로필에서는 `pms_personal_playlist`, `pms_personal_playlist_track`에 저장한다
- `local` 프로필에서는 인메모리 저장소에 저장한다
- 저장 가능한 track은 반드시 사용자의 정식 `PMS user library`에 있어야 한다
- 같은 playlist에 같은 track을 다시 저장하면 중복 추가하지 않고 현재 playlist 상태를 반환한다
- GMS 화면의 `Save` 버튼은 먼저 GMS feedback을 남긴 뒤 기본 personal playlist에 track을 저장한다

## 다음 연결 지점

1. 특정 personal playlist를 선택해 GMS 후보 저장
2. personal playlist 상세 화면과 track 삭제/정렬
3. 개인 playlist를 연결된 외부 플랫폼으로 export
