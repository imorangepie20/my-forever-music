# Platform OAuth Sandbox API

작성일: `2026-05-03`

이 문서는 `플랫폼 연결 온보딩` 안에서 사용하는 sandbox OAuth 시작/완료 공개 API 계약입니다.

## 목적

- 실제 플랫폼 OAuth 이전에 `start -> approve -> callback -> complete` 흐름을 먼저 고정
- 웹앱이 승인 화면과 callback 화면을 통해 연결 과정을 재현할 수 있게 하기
- 이후 실제 외부 플랫폼 OAuth로 바꾸더라도 현재 계약과 상태 전이를 기준점으로 남기기

## 엔드포인트

- `POST /api/v1/platforms/oauth/start`
- `POST /api/v1/platforms/oauth/complete`

## OAuth 시작

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

### 예시 응답

```json
{
  "service": "api",
  "status": "authorization_pending",
  "generated_at": "2026-05-03T08:00:00Z",
  "user": {
    "user_id": "user-001",
    "display_name": "Forever Listener",
    "email": "listener@example.com"
  },
  "authorization": {
    "state": "oauth-test-state",
    "platform_id": "spotify",
    "platform_display_name": "Spotify",
    "authorization_mode": "sandbox-oauth",
    "authorization_channel": "internal_approval_page",
    "requested_scopes": ["playlist-read", "profile-read"],
    "expires_at": "2026-05-03T08:10:00Z",
    "approval_page_path": "/platforms/oauth/authorize?state=oauth-test-state",
    "callback_path": "/platforms/oauth/callback?state=oauth-test-state&code=sandbox-approved",
    "sandbox_approval_code": "sandbox-approved",
    "external_authorization_url": null,
    "redirect_uri": null
  }
}
```

## OAuth 완료

### 요청 필드

- `user_id`
- `platform_id`
- `state`
- `approval_code`

### 예시 요청

```json
{
  "user_id": "user-001",
  "platform_id": "spotify",
  "state": "oauth-test-state",
  "approval_code": "sandbox-approved"
}
```

## 현재 구현 메모

- `local` 프로필에서는 pending authorization session을 in-memory 저장소에 둔다
- `database` 같은 DB 활성 프로필에서는 `platform_authorization_session` 테이블을 사용할 수 있도록 자리가 준비되어 있다
- 현재는 외부 플랫폼 페이지가 아니라 웹앱 내부의 `/platforms/oauth/authorize`, `/platforms/oauth/callback` 라우트를 사용한다
- 승인 완료 후에는 내부적으로 platform connection store와 platform credential store를 함께 갱신한다
- preferred platform이 연결되고 credential까지 준비되면 다음 단계가 `/pms`가 된다
- `Spotify`는 설정값이 채워지면 `sandbox-oauth` 대신 `spotify-pkce-draft` 모드로 시작 URL을 생성할 수 있다
- 이 경우 `authorization_channel`은 `external_browser_redirect`가 되고, 응답에 `external_authorization_url`, `redirect_uri`가 포함된다
- 현재 `external_browser_redirect` 모드는 provider callback의 `authorization code`를 받아 token exchange client를 호출하고, 응답으로 받은 `access token / refresh token / scope / expires_at`을 credential 저장소에 기록한다
- Spotify draft 모드는 PKCE `code_verifier`를 authorization session에 보관했다가 token 교환 시 사용한다

## 다음 연결 지점

1. 실제 외부 OAuth authorize URL과 callback URL로 교체
2. state 검증을 토큰 저장소와 연동
3. 플랫폼별 access token, refresh token, scope 만료 정보를 분리 저장
