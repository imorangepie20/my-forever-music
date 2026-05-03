# Last.fm Signal Preview API

작성일: `2026-05-04`

## 목적

`Last.fm`을 `PMS import` 플랫폼이 아니라 `장기 청취/affinity 신호` 플랫폼으로 쓰기 위해, 공개 사용자명 기준으로 최근 scrobble과 상위 아티스트/트랙을 미리 확인하는 API입니다.

이 응답은 현재 `Platforms` 화면에서 preview 패널로 쓰이고, 이후 `EMS/GMS` seed 보강과 장기 취향 모델 입력으로 이어질 기준 계약입니다.

## 엔드포인트

- `GET /api/v1/platforms/lastfm/preview`

## 요청

### Query Parameters

- `username`: 필수. Last.fm 공개 사용자명
- `period`: 선택. 기본값 `1month`
  - 허용값: `overall`, `7day`, `1month`, `3month`, `6month`, `12month`
- `recent_limit`: 선택. 기본값 `8`
- `top_limit`: 선택. 기본값 `6`

### 예시

```http
GET /api/v1/platforms/lastfm/preview?username=mibeen&period=1month&recent_limit=8&top_limit=6
```

## 응답

### 예시

```json
{
  "service": "api",
  "status": "ok",
  "generated_at": "2026-05-04T03:10:00Z",
  "request": {
    "username": "mibeen",
    "period": "1month",
    "recent_limit": 8,
    "top_limit": 6
  },
  "user": {
    "username": "mibeen",
    "real_name": "Woo Sung Jo",
    "country": "KR",
    "playcount": 54189,
    "profile_url": "https://www.last.fm/user/mibeen",
    "avatar_url": null,
    "registered_at": "2020-01-01T00:00:00Z"
  },
  "summary": {
    "source": "lastfm-public-api",
    "recent_track_count": 8,
    "top_artist_count": 6,
    "top_track_count": 6,
    "now_playing": true,
    "distinct_recent_artist_count": 5,
    "next_step_message": "Use top artists as EMS affinity seeds or keep Last.fm as a long-term taste signal source."
  },
  "insights": [
    {
      "insight_id": "artist-anchor",
      "title": "Long-Term Artist Anchor",
      "detail": "The Midnight is the strongest 1month artist signal right now with 88 plays."
    }
  ],
  "recent_tracks": [
    {
      "track_name": "Days of Thunder",
      "artist_name": "The Midnight",
      "album_name": "Days of Thunder",
      "track_url": "https://www.last.fm/music/The+Midnight/_/Days+of+Thunder",
      "image_url": null,
      "now_playing": true,
      "played_at": null,
      "loved": true
    }
  ],
  "top_artists": [
    {
      "artist_name": "The Midnight",
      "rank": 1,
      "playcount": 88,
      "artist_url": "https://www.last.fm/music/The+Midnight",
      "image_url": null
    }
  ],
  "top_tracks": [
    {
      "track_name": "Days of Thunder",
      "artist_name": "The Midnight",
      "rank": 1,
      "playcount": 24,
      "track_url": "https://www.last.fm/music/The+Midnight/_/Days+of+Thunder",
      "artist_url": "https://www.last.fm/music/The+Midnight",
      "image_url": null
    }
  ]
}
```

## 현재 구현 메모

- 이 API는 `Last.fm public user methods`를 사용하므로, 현재 단계에서는 사용자 로그인 세션 없이도 preview를 볼 수 있습니다.
- 서버에는 `LASTFM_ENABLED=true`, `LASTFM_API_KEY`가 설정되어 있어야 합니다.
- 현재 preview는 `user.getInfo`, `user.getRecentTracks`, `user.getTopArtists`, `user.getTopTracks` 결과를 합쳐서 반환합니다.
- `apps/web`의 `/platforms` 화면은 이 응답을 받아 preview를 보여주고, `top_artists`를 EMS seed artist로 복사할 수 있습니다.
- 현재 `apps/web`는 이 preview 결과를 계정에 저장하는 `POST /api/v1/platforms/lastfm/profile` 흐름도 함께 사용합니다.
- 저장된 `Last.fm username`은 이후 `EMS analysis`에서 `top artist` 신호를 자동 blend 하는 입력으로도 사용됩니다.
- 아직 `scrobble 장기 동기화 배치`나 `공식 인증 기반 세션 연결`은 구현되지 않았습니다.

## 다음 연결 지점

1. Preview 결과를 장기 사용자 모델과 GMS ranking 신호에 더 직접 연결
2. Last.fm 최근 scrobble을 배치 적재해 시계열 취향 변화도 반영
3. Spotify 매칭과 결합해 Last.fm track 신호를 오디오 특성 파이프라인으로 보강
