# Platform Connection Onboarding API

작성일: `2026-05-03`

이 문서는 회원가입 직후 사용자의 스트리밍 플랫폼 연결 상태를 확인하고, 로컬 온보딩 환경에서 연결과 해제를 수행하는 공개 API 계약입니다.

## 목적

- 가입이 끝난 사용자의 `플랫폼 연결 상태`를 조회
- preferred platform이 연결되었는지 판단
- 다음 단계인 `PMS import`로 넘어갈 수 있는지 명시

## 엔드포인트

- `GET /api/v1/platforms/connections/bootstrap?user_id={user_id}`
- `POST /api/v1/platforms/connections/connect`
- `POST /api/v1/platforms/connections/disconnect`

## 연결 상태 조회

### 요청

- query parameter: `user_id`

### 예시 응답

```json
{
  "service": "api",
  "status": "ok",
  "generated_at": "2026-05-03T07:00:00Z",
  "user": {
    "user_id": "user-001",
    "display_name": "Forever Listener",
    "email": "listener@example.com",
    "preferred_platform_id": "spotify"
  },
  "summary": {
    "connected_platform_count": 1,
    "preferred_platform_connected": true,
    "preferred_platform_reconnect_required": false,
    "onboarding_stage": "import-playlists",
    "next_step_path": "/pms",
    "next_step_message": "Preferred platform is connected. You can continue into PMS import."
  },
  "connections": [
    {
      "platform_id": "spotify",
      "display_name": "Spotify",
      "preferred": true,
      "connected": true,
      "connection_status": "connected",
      "connection_mode": "sandbox",
      "external_account_label": "Forever Listener Spotify account",
      "sync_ready": true,
      "credential_status": "ready",
      "reconnect_required": false,
      "connected_at": "2026-05-03T07:00:00Z",
      "next_action_label": "Disconnect"
    }
  ]
}
```

## 플랫폼 연결

### 요청 필드

- `user_id`
- `platform_id`
- `connection_mode`
- `external_account_label`

### 예시 요청

```json
{
  "user_id": "user-001",
  "platform_id": "spotify",
  "connection_mode": "sandbox",
  "external_account_label": "Forever Listener Spotify account"
}
```

## 플랫폼 해제

### 요청 필드

- `user_id`
- `platform_id`

### 예시 요청

```json
{
  "user_id": "user-001",
  "platform_id": "spotify"
}
```

## 현재 구현 메모

- `local` 프로필에서는 in-memory 연결 저장소를 사용한다
- `database` 같은 DB 활성 프로필에서는 `platform_account_connection` 테이블을 사용할 수 있도록 자리가 준비되어 있다
- 웹앱 기준 연결 시작은 현재 `sandbox OAuth` 승인 흐름으로 진행된다
- preferred platform이 연결되면 다음 단계가 `/pms`로 바뀐다
- 저장된 credential이 refresh 실패나 만료로 usable 하지 않으면 `preferred_platform_reconnect_required=true`가 되고, 카드의 `next_action_label`은 `Reconnect`로 바뀐다
- 이 상태는 `connected=true`일 수 있지만 `sync_ready=false`이므로, PMS import는 계속 막히고 사용자는 `/platforms`에서 다시 OAuth를 시작해야 한다

## 다음 연결 지점

1. 연결 성공 후 PMS playlist import job 시작 API를 연결
2. 연결 scope, token 상태, 최근 sync 시각을 실제 플랫폼 데이터와 묶기
3. 상세 OAuth start/complete 흐름은 [PLATFORM_OAUTH_SANDBOX_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PLATFORM_OAUTH_SANDBOX_API.md) 를 따른다
