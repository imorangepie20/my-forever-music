# EMS Workspace Analysis API

작성일: `2026-05-02`

이 문서는 `services/api`가 `EMS` 화면 기본 제어값을 계산할 때 사용하는 공개 API 계약입니다.

## 목적

- `PMS`에서 고른 시드를 바탕으로 `EMS` 기본 `mood / energy / familiarity bias`를 추천
- 프론트가 rule-based 기준값을 먼저 사용할 수 있게 하기
- 이후 AI 분석과 결합되더라도 현재의 서버 기준 입력/출력 형식을 먼저 고정하기

## 엔드포인트

- `POST /api/v1/ems/workspace/analysis`

## 요청

### 요청 필드

- `user_id`
- `playlist_id`
- `seed_track_ids`
- `seed_artist_names`
- `seed_genres`

### 예시 요청

```json
{
  "user_id": "user-001",
  "playlist_id": "playlist-001",
  "seed_track_ids": ["track-alpha", "track-beta"],
  "seed_artist_names": ["Artist One"],
  "seed_genres": ["synth-pop", "dream-pop"]
}
```

## 응답

### 응답 필드

- `service`
- `status`
- `generated_at`
- `context`
  - `strategy`
  - `playlist_id`
  - `track_seed_count`
  - `artist_seed_count`
  - `genre_seed_count`
  - `matched_catalog_track_count`
- `workspace_recommendation`
  - `mood`
  - `energy_level`
  - `familiarity_bias`
  - `confidence_score`
- `top_signals`
- `notes`
- `warnings`

### 예시 응답

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

## 현재 구현 메모

- `local` 프로필에서는 DB 없이도 입력된 텍스트 시드만으로 분석합니다.
- `database` 프로필에서는 `PMS` 카탈로그와 seed track 매칭을 시도하고, 매칭 성공 수를 `matched_catalog_track_count`로 돌려줍니다.
- 아직 AI 서비스 호출은 하지 않습니다. 이 단계는 `EMS` 제어값을 안정적으로 정하는 서버 기준값을 만드는 목적입니다.

## 다음 연결 지점

1. `services/ai`의 추천 보강 결과와 EMS 제어값 산출을 결합
2. 사용자 행동 이벤트를 반영한 EMS bias 조정 규칙 추가
3. rule-based 분석기를 모델 기반 scorer로 단계적으로 교체
