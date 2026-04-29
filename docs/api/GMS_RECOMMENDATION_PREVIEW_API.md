# GMS Recommendation Preview API

작성일: `2026-04-29`

이 문서는 `services/api`가 외부 클라이언트에 제공하는 GMS 추천 preview 엔드포인트 초안입니다.

## 목적

- 웹앱과 데스크탑 앱이 `services/api`만 바라보고 추천 preview를 요청할 수 있게 하기
- `services/api`와 `services/ai` 사이의 첫 오케스트레이션 경로를 고정하기
- 실제 인증, 사용자 컨텍스트, 카탈로그 연동 전에도 클라이언트 계약부터 확정하기

## 엔드포인트

- 공개 경로: `POST /api/v1/gms/recommendations/preview`
- 내부 위임 경로: `POST /v1/recommendations/preview` on `services/ai`

## 요청 형식

현재 요청/응답 JSON은 `services/ai` 계약과 같은 `snake_case`를 사용합니다.

이 경로는 현재 `GMS` preview 전용이므로 `mode`는 생략하거나 `gms`만 허용합니다.

예시:

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

## 응답 형식

- 현재는 `services/ai` 응답을 거의 그대로 전달
- 추후 `services/api`에서 인증/사용자 컨텍스트/카탈로그 보강이 들어오면 이 계약이 확장될 수 있음

## 설정값

- `AI_SERVICE_BASE_URL`
  - 기본값: `http://localhost:8000`
- `AI_RECOMMENDATION_PREVIEW_PATH`
  - 기본값: `/v1/recommendations/preview`

## 오류 처리

- AI 서비스가 비어 있는 응답을 반환하면 `502 Bad Gateway`
- AI 서비스가 응답 오류를 내면 `502 Bad Gateway`
- AI 서비스에 연결할 수 없으면 `502 Bad Gateway`
- 요청 validation 실패 시 `400 Bad Request`

## 연결 관계

1. Client -> `services/api`
2. `services/api` -> `services/ai`
3. `services/ai` -> preview recommendation response

상세 AI 계약은 [AI_RECOMMENDATION_PREVIEW.md](/Users/woosungjo/music-space/my-forever-music/docs/api/AI_RECOMMENDATION_PREVIEW.md) 를 본다.
