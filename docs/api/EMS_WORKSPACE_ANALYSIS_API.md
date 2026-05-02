# EMS Workspace Analysis API

## Endpoint

- `POST /api/v1/ems/workspace/analysis`

## Purpose

`PMS`에서 고른 시드 트랙, 아티스트, 장르를 바탕으로 `EMS` 화면의 기본 `mood / energy / familiarity bias`를 추천합니다. 현재 구현은 `Spring Boot` 내부의 rule-based 분석기와 로컬 `PMS` 카탈로그를 함께 사용합니다.

## Request

```json
{
  "user_id": "user-001",
  "playlist_id": "playlist-001",
  "seed_track_ids": ["track-alpha", "track-beta"],
  "seed_artist_names": ["Artist One"],
  "seed_genres": ["synth-pop", "dream-pop"]
}
```

## Response

```json
{
  "service": "api",
  "status": "ok",
  "generated_at": "2026-05-02T09:00:00Z",
  "context": {
    "strategy": "catalog-signal-analysis-v1",
    "playlist_id": "playlist-001",
    "track_seed_count": 2,
    "artist_seed_count": 1,
    "genre_seed_count": 2,
    "matched_catalog_track_count": 2
  },
  "workspace_recommendation": {
    "mood": "upbeat",
    "energy_level": 4,
    "familiarity_bias": 4,
    "confidence_score": 0.86
  },
  "top_signals": [
    {
      "type": "genre",
      "label": "synth-pop",
      "weight": 1.8,
      "reason": "Synth-pop pushes the session toward repeatable uplift and bright momentum."
    }
  ],
  "notes": [
    "synth-pop carries the strongest lift, so EMS is biasing toward upbeat motion."
  ],
  "warnings": []
}
```

## Behavior

- `local` 프로필에서는 DB 없이도 입력된 텍스트 시드만으로 분석합니다.
- `database` 프로필에서는 `PMS` 카탈로그와 seed track 매칭을 시도하고, 매칭 성공 수를 `matched_catalog_track_count`로 돌려줍니다.
- 아직 AI 서비스 호출은 하지 않습니다. 이 단계는 `EMS` 제어값을 안정적으로 정하는 서버 기준값을 만드는 목적입니다.
