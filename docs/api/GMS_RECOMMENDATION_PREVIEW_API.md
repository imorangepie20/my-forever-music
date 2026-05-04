# GMS Recommendation Preview API

작성일: `2026-04-29`

이 문서는 `services/api`가 외부 클라이언트에 제공하는 GMS 추천 preview 공개 API 계약입니다.

## 목적

- 웹앱과 데스크탑 앱이 `services/api`만 바라보고 추천 preview를 요청할 수 있게 하기
- `services/api`와 `services/ai` 사이의 첫 오케스트레이션 경로를 고정하기
- 실제 인증, 사용자 컨텍스트, 카탈로그 연동 전에도 클라이언트 계약부터 확정하기

## 엔드포인트

- 공개 경로: `POST /api/v1/gms/recommendations/preview`
- 내부 위임 경로: `POST /v1/recommendations/preview` on `services/ai`

## 요청

현재 요청/응답 JSON은 `services/ai` 계약과 같은 `snake_case`를 사용합니다.

이 경로는 현재 `GMS` preview 전용이므로 `mode`는 생략하거나 `gms`만 허용합니다.

### 요청 필드

- `request_id`
- `user_id`
- `playlist_id`
- `mode`
- `mood`
- `energy_level`
- `familiarity_bias`
- `limit`
- `seed_track_ids`
- `seed_artist_names`
- `seed_genres`
- `include_explanations`

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

### 응답 구조

- 현재는 `services/ai` 응답을 거의 그대로 전달
- 추후 `services/api`에서 인증, 사용자 컨텍스트, 카탈로그 보강이 들어오면 이 계약이 확장될 수 있음
- 상세 필드 구조는 [AI_RECOMMENDATION_PREVIEW.md](/Users/woosungjo/music-space/my-forever-music/docs/api/AI_RECOMMENDATION_PREVIEW.md) 를 따른다
- 현재 `items[*]`는 아래 media metadata까지 포함한다
  - `source_platform`
  - `source_playlist_id`
  - `source_playlist_title`
  - `album_title`
  - `album_image_url`
  - `platform_external_url`
  - `platform_uri`
  - `preview_url`
  - `spotify_track_id`
  - `duration_ms`

## 현재 구현 메모

- `services/api`는 현재 AI 서비스 응답을 거의 그대로 브리지하지만, 저장된 `Last.fm profile`이 있으면 먼저 저장된 `scrobble snapshot`의 최근 artist recurrence를 `seed_artist_names`에 자동 blend 한다
- 저장된 scrobble snapshot이 비어 있으면 live `Last.fm top artists` 조회로 fallback 한다
- 현재 `services/api`는 AI preview 응답을 받은 뒤, 가능하면 현재 사용자의 `PMS user library`에서 실제 트랙을 다시 매칭해 playable item으로 재투영한다
- AI preview가 후보를 반환했는데 PMS user library에서 실제 playable track으로 재매핑하지 못하면 가짜 후보를 그대로 노출하지 않고 요청을 실패시킨다
- 이때 선택된 `playlist_id`와 seed track/artist/genre, seed 여부, audio feature energy alignment를 함께 사용해 우선순위를 계산한다
- 따라서 현재 `GMS` 카드와 공통 플레이어는 synthetic track이 아니라 실제 PMS 라이브러리 트랙을 기준으로 동작한다
- 내부 호출 대상은 `AI_SERVICE_BASE_URL`과 `AI_RECOMMENDATION_PREVIEW_PATH` 설정으로 바꿀 수 있다
- `mode`가 비어 있으면 `gms`로 해석하는 사용 흐름을 전제로 한다
- 이때 추가된 `Last.fm` artist는 AI 응답의 `input_summary.artist_seed_count`, `context.seed_basis`, `warnings`에도 반영될 수 있다

### 설정값

- `AI_SERVICE_BASE_URL`
  - 기본값: `http://localhost:8000`
- `AI_RECOMMENDATION_PREVIEW_PATH`
  - 기본값: `/v1/recommendations/preview`

### 오류 처리

- AI 서비스가 비어 있는 응답을 반환하면 `502 Bad Gateway`
- AI 서비스가 응답 오류를 내면 `502 Bad Gateway`
- AI 서비스에 연결할 수 없으면 `502 Bad Gateway`
- PMS user library가 없어 playable item을 만들 수 없으면 `400 Bad Request`
- 요청 validation 실패 시 `400 Bad Request`

## 다음 연결 지점

1. Last.fm 외에도 PMS import 히스토리, EMS session state를 함께 GMS 입력에 주입
2. preview 결과를 실제 트랙 카탈로그와 연결
3. GMS 평가 결과를 다시 PMS 학습 데이터로 환류
