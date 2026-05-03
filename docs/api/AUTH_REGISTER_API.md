# Auth Register API

작성일: `2026-05-03`

이 문서는 `my-forever-music`의 첫 번째 회원가입 공개 API 계약입니다.

## 목적

- 사용자의 계정 기본 정보를 생성
- 회원가입 시점에 `기본 스트리밍 플랫폼 선택`을 함께 받기
- 가입 직후 다음 단계가 `플랫폼 연결`이라는 온보딩 상태를 명시적으로 돌려주기

## 엔드포인트

- `POST /api/v1/auth/register`

## 요청

### 요청 필드

- `display_name`
- `email`
- `password`
- `preferred_platform_id`
  - 현재 허용값: `spotify`, `apple-music`, `tidal`, `youtube-music`, `last-fm`
- `marketing_opt_in`
- `accepted_terms`
- `accepted_privacy_policy`

### 예시 요청

```json
{
  "display_name": "Forever Listener",
  "email": "listener@example.com",
  "password": "music2026",
  "preferred_platform_id": "spotify",
  "marketing_opt_in": true,
  "accepted_terms": true,
  "accepted_privacy_policy": true
}
```

## 응답

### 응답 필드

- `service`
- `status`
- `registered_at`
- `user`
  - `user_id`
  - `email`
  - `display_name`
  - `email_verified`
- `onboarding`
  - `stage`
  - `preferred_platform_id`
  - `platform_connection_required`
  - `next_step_path`
  - `next_step_message`

### 예시 응답

```json
{
  "service": "api",
  "status": "registered",
  "registered_at": "2026-05-03T03:00:00Z",
  "user": {
    "user_id": "user-5ef49c86-476d-4d7a-a4cc-85f9c55d272e",
    "email": "listener@example.com",
    "display_name": "Forever Listener",
    "email_verified": false
  },
  "onboarding": {
    "stage": "connect-platform",
    "preferred_platform_id": "spotify",
    "platform_connection_required": true,
    "next_step_path": "/platforms",
    "next_step_message": "Connect your streaming service to start PMS import."
  }
}
```

## 현재 구현 메모

- 기본 `local` 프로필에서는 in-memory 저장소를 사용하므로 DB 없이도 바로 동작한다
- `database` 같은 DB 활성 프로필에서는 `auth_user_account` 테이블과 JPA 저장소를 사용하도록 자리가 준비되어 있다
- 비밀번호는 Spring Security `BCryptPasswordEncoder`로 해시된다
- 중복 이메일은 `409 Conflict`로 응답한다
- 요청 validation 실패는 공통 에러 포맷으로 `400 Bad Request`를 돌려준다
- 가입 성공 후 `/platforms`에서 사용할 수 있도록 `platform connection onboarding` 단계가 이어진다
- `last-fm`을 preferred platform으로 선택할 수는 있지만, 현재 단계에서는 PMS playlist import보다 청취 이력/affinity 신호 수집용 플랫폼으로 간주한다

## 다음 연결 지점

1. 가입 직후 실제 플랫폼 OAuth 연결 시작 API 추가
2. 이메일 인증 또는 세션/토큰 발급 단계 추가
3. 사용자 설정과 PMS owner를 동일 사용자 엔터티로 연결
4. 상세 연결 흐름은 [PLATFORM_CONNECTION_ONBOARDING_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PLATFORM_CONNECTION_ONBOARDING_API.md) 를 따른다
