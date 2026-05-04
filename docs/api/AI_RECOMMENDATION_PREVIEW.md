# AI Recommendation Preview API

작성일: `2026-04-29`

이 문서는 `services/ai`가 제공하는 첫 번째 추천 preview 내부 계약입니다.

## 목적

- `services/api`가 AI 서비스와 연동할 때 사용할 기본 요청/응답 형태를 먼저 고정
- PMS / EMS / GMS 흐름에서 추천 결과를 어떻게 표현할지 공통 기준 마련
- 실제 모델 연동 전에도 프론트와 백엔드가 계약 기반으로 병렬 작업 가능하게 하기

## 엔드포인트

- 내부 경로: `POST /v1/recommendations/preview`
- Nginx 프록시 뒤 공개 경로: `POST /ai/v1/recommendations/preview`

## 요청

요청과 응답 JSON은 현재 `snake_case`를 사용합니다.

### 요청 필드

- `request_id`: 호출 추적용 식별자, 선택
- `user_id`: 사용자 식별자, 선택
- `playlist_id`: 플레이리스트 식별자, 선택
- `mode`: `pms | ems | gms | discovery`
- `mood`: `focus | calm | upbeat | melancholy | discovery`
- `energy_level`: `1..5`, 선택
- `familiarity_bias`: `1..5`, 기본값 `3`
- `limit`: `1..20`, 기본값 `10`
- `seed_track_ids`: 트랙 시드 목록
- `seed_artist_names`: 아티스트 시드 목록
- `seed_genres`: 장르 시드 목록
- `include_explanations`: 설명 문자열 포함 여부

### 예시 요청

```json
{
  "request_id": "preview-001",
  "user_id": "user-{uuid}",
  "playlist_id": "pms-spotify-{spotify_playlist_id}",
  "mode": "gms",
  "mood": "upbeat",
  "energy_level": 4,
  "familiarity_bias": 3,
  "limit": 3,
  "seed_track_ids": ["pms-track-spotify-{spotify_track_id}"],
  "seed_artist_names": ["Imported Artist"],
  "seed_genres": ["imported-genre"],
  "include_explanations": true
}
```

## 응답

### 응답 필드

- `request_id`: 최종 추적 id
- `generated_at`: 생성 시각
- `service`: 항상 `ai`
- `status`: 현재는 `ok`
- `context.strategy`: 추천 전략
- `context.engine`: 현재 엔진 식별자
- `context.mode`: 사용된 추천 공간
- `context.mood`: 사용된 mood
- `context.energy_level`: 최종 적용 에너지 레벨
- `context.seed_basis`: 내부 생성에 사용된 정규화 seed 목록
- `input_summary`: 입력 요약
- `items`: 추천 후보 목록
- `warnings`: 보강/재시도 안내 메시지

### 전략 값

- `pms-seed-match`
- `ems-mood-match`
- `gms-hybrid-blend`
- `discovery-fallback`

### 예시 응답

```json
{
  "request_id": "preview-001",
  "generated_at": "2026-04-29T12:00:00Z",
  "service": "ai",
  "status": "ok",
  "context": {
    "strategy": "gms-hybrid-blend",
    "engine": "rule-based-preview-v1",
    "mode": "gms",
    "mood": "upbeat",
    "energy_level": 4,
    "seed_basis": ["pms-track-spotify-{spotify_track_id}", "imported-artist", "imported-genre"]
  },
  "input_summary": {
    "user_id": "user-{uuid}",
    "playlist_id": "pms-spotify-{spotify_playlist_id}",
    "track_seed_count": 1,
    "artist_seed_count": 1,
    "genre_seed_count": 1,
    "familiarity_bias": 3,
    "limit": 3
  },
  "items": [
    {
      "rank": 1,
      "track_id": "ai-preview-candidate-01",
      "title": "AI Preview Candidate",
      "artist_name": "Imported Artist",
      "score": 0.97,
      "source_space": "gms",
      "energy_level": 4,
      "reason": "This preview candidate was selected by gms-hybrid-blend to support an upbeat listening flow and stays close to the supplied PMS seeds at rank 1."
    }
  ],
  "warnings": []
}
```

## 현재 구현 메모

- 실제 음원 카탈로그 조회는 하지 않으므로, 사용자-facing playable 추천은 `services/api`가 PMS user library로 재매핑한 결과를 기준으로 한다
- 벡터 검색, 임베딩, 모델 추론이 아직 연결되지 않음
- 점수는 preview 단계의 규칙 기반 값임
- 현재 `services/api`의 `POST /api/v1/gms/recommendations/preview`가 이 계약을 내부적으로 호출한다

## 다음 연결 지점

1. `services/api`에서 이 계약으로 내부 HTTP 호출 시작
2. 추천 결과를 실제 트랙/아티스트 데이터와 매핑
3. preview 엔진을 모델 기반 ranking 엔진으로 교체
