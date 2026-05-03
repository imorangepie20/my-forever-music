# Auth Login API

작성일: `2026-05-04`

## 목적

- 기존 계정이 이메일과 비밀번호로 다시 로그인할 수 있게 하기
- MacBook 로컬 시험 서비스 중 브라우저 세션을 다시 복원할 수 있게 하기
- 로그인 시점의 실제 온보딩 다음 단계를 함께 반환하기

## 엔드포인트

- `POST /api/v1/auth/login`

## 요청

### 요청 필드

- `email`
- `password`

### 예시 요청

```json
{
  "email": "listener@example.com",
  "password": "music2026"
}
```

## 응답

### 응답 필드

- `service`
- `status`
- `authenticated_at`
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
  "status": "authenticated",
  "authenticated_at": "2026-05-04T02:30:00Z",
  "user": {
    "user_id": "user-001",
    "email": "listener@example.com",
    "display_name": "Forever Listener",
    "email_verified": false
  },
  "onboarding": {
    "stage": "import-playlists",
    "preferred_platform_id": "spotify",
    "platform_connection_required": false,
    "next_step_path": "/pms",
    "next_step_message": "Preferred platform is connected. You can continue into PMS import."
  }
}
```

## 현재 구현 메모

- 이메일은 소문자 normalize 후 조회합니다.
- 비밀번호는 저장된 `BCrypt` 해시와 비교합니다.
- 로그인 성공 시 `Platform Connection Bootstrap`을 다시 계산해서 현재 기준 다음 단계(`Platforms` 또는 `PMS`)를 반환합니다.
- 잘못된 이메일/비밀번호는 `401 Unauthorized`와 `invalid_credentials` 코드로 응답합니다.
- 이 API는 아직 토큰 발급 단계가 아니라, 로컬 시험 서비스용 `session restore` 성격의 인증 API입니다.

## 다음 연결 지점

1. 토큰 또는 세션 발급 단계 추가
2. 이메일 인증 여부와 비밀번호 재설정 흐름 추가
3. 이후 보호된 API 접근 제어와 연결
