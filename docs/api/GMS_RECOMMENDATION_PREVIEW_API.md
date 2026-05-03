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
  "user_id": "user-123",
  "playlist_id": "playlist-001",
  "mode": "gms",
  "mood": "upbeat",
  "energy_level": 4,
  "familiarity_bias": 3,
  "limit": 3,
  "seed_track_ids": ["track-alpha", "track-beta"],
  "seed_artist_names": ["Artist One"],
  "seed_genres": ["synth-pop"],
  "include_explanations": true
}
```

## 응답

### 응답 구조

- 현재는 `services/ai` 응답을 거의 그대로 전달
- 추후 `services/api`에서 인증, 사용자 컨텍스트, 카탈로그 보강이 들어오면 이 계약이 확장될 수 있음
- 상세 필드 구조는 [AI_RECOMMENDATION_PREVIEW.md](/Users/woosungjo/music-space/my-forever-music/docs/api/AI_RECOMMENDATION_PREVIEW.md) 를 따른다

## 현재 구현 메모

- `services/api`는 현재 AI 서비스 응답을 거의 변형 없이 브리지한다
- 내부 호출 대상은 `AI_SERVICE_BASE_URL`과 `AI_RECOMMENDATION_PREVIEW_PATH` 설정으로 바꿀 수 있다
- `mode`가 비어 있으면 `gms`로 해석하는 사용 흐름을 전제로 한다

### 설정값

- `AI_SERVICE_BASE_URL`
  - 기본값: `http://localhost:8000`
- `AI_RECOMMENDATION_PREVIEW_PATH`
  - 기본값: `/v1/recommendations/preview`

### 오류 처리

- AI 서비스가 비어 있는 응답을 반환하면 `502 Bad Gateway`
- AI 서비스가 응답 오류를 내면 `502 Bad Gateway`
- AI 서비스에 연결할 수 없으면 `502 Bad Gateway`
- 요청 validation 실패 시 `400 Bad Request`

## 다음 연결 지점

1. 인증된 사용자 컨텍스트와 저장된 PMS/EMS 상태를 이 요청에 자동 주입
2. preview 결과를 실제 트랙 카탈로그와 연결
3. GMS 평가 결과를 다시 PMS 학습 데이터로 환류
