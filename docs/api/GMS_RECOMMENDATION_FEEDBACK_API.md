# GMS Recommendation Feedback API

작성일: `2026-05-05`

이 문서는 GMS 추천 후보에 대한 사용자 평가를 저장하는 공개 API 계약입니다.

## 목적

- 추천 결과를 단순 표시가 아니라 `평가 -> PMS 학습 신호 -> 재추천` 루프로 연결
- 좋아요, 싫어요, 저장, 스킵 같은 사용자의 명시적 반응을 사용자별 데이터로 보존
- 가짜 추천 후보가 아니라 PMS user library에 있는 실제 track에 대해서만 평가를 저장

## 엔드포인트

- 공개 경로: `POST /api/v1/gms/recommendations/feedback`

## 요청

```json
{
  "user_id": "user-{uuid}",
  "request_id": "web-preview-1770000000000",
  "playlist_id": "pms-spotify-{spotify_playlist_id}",
  "track_id": "pms-track-spotify-{spotify_track_id}",
  "feedback_type": "like",
  "score": 1,
  "source_space": "gms",
  "reason": "The candidate matched the current PMS playlist context."
}
```

### 요청 필드

- `user_id`: 필수
- `request_id`: 선택, GMS preview 요청 ID
- `playlist_id`: 선택, 추천 후보가 연결된 PMS playlist ID
- `track_id`: 필수, PMS user library에 저장된 track ID
- `feedback_type`: 필수, `like`, `dislike`, `save`, `skip`
- `score`: 선택, 현재 웹앱은 긍정 신호 `1`, 부정 신호 `-1`을 보냄
- `source_space`: 선택, 추천 후보 출처
- `reason`: 선택, 추천 후보 설명 snapshot

## 응답

```json
{
  "service": "gms-recommendation-feedback",
  "status": "recorded",
  "processed_at": "2026-05-05T00:00:00Z",
  "feedback": {
    "feedback_id": 1,
    "user_id": "user-{uuid}",
    "request_id": "web-preview-1770000000000",
    "playlist_id": "pms-spotify-{spotify_playlist_id}",
    "track_id": "pms-track-spotify-{spotify_track_id}",
    "feedback_type": "like",
    "score": 1,
    "source_space": "gms",
    "reason": "The candidate matched the current PMS playlist context.",
    "created_at": "2026-05-05T00:00:00Z"
  },
  "next_step_message": "Feedback is now available as PMS learning signal input for future model iterations."
}
```

## 현재 구현 메모

- DB 활성 프로필에서는 `gms_recommendation_feedback` 테이블에 저장한다
- `local` 프로필에서는 인메모리 저장소에 저장한다
- 저장 전 `track_id`가 해당 사용자의 PMS user library에 존재하는지 확인한다
- 존재하지 않는 track에 대한 feedback은 `400 Bad Request`로 거절한다

## 다음 연결 지점

1. GMS feedback을 사용자별 음악 학습 모델 입력으로 반영
2. 추천 후보 저장을 사용자 제작 playlist 생성/추가 흐름으로 확장
3. 공통 플레이어의 재생 완료, 스킵, 반복 이벤트도 같은 학습 신호 계열로 저장
