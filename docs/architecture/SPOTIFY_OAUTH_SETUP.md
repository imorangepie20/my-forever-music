# Spotify OAuth Setup

작성일: `2026-05-03`

이 문서는 `my-forever-music`의 Spotify OAuth 설정값과 실행 기준을 정리합니다.

## 목적

- Spotify Dashboard에 등록해야 할 Redirect URI를 고정하기
- `services/api`에서 어떤 환경 변수를 써야 하는지 정리하기
- 브라우저에서 Spotify OAuth를 테스트하기 전에 필요한 HTTPS 조건을 명확히 하기

## 현재 기준 Redirect URI

현재 프로젝트 구현의 프론트 콜백 경로는 아래 하나입니다.

```text
/platforms/oauth/callback
```

외부 도메인 기준 Spotify Dashboard 등록값은 아래입니다.

```text
https://imapplepie20.tplinkdns.com/platforms/oauth/callback
```

Spotify OAuth는 `redirect_uri`가 등록값과 정확히 일치해야 합니다.

## 환경 변수

`services/api/.env.local` 기준:

```bash
API_PORT=8081
AI_SERVICE_BASE_URL=http://localhost:8000
SPOTIFY_OAUTH_ENABLED=true
SPOTIFY_CLIENT_ID=...
SPOTIFY_CLIENT_SECRET=...
SPOTIFY_REDIRECT_URI=https://imapplepie20.tplinkdns.com/platforms/oauth/callback
SPOTIFY_AUTHORIZATION_URI=https://accounts.spotify.com/authorize
SPOTIFY_TOKEN_URI=https://accounts.spotify.com/api/token
SPOTIFY_API_BASE_URI=https://api.spotify.com/v1
```

현재 구현에서 실제 필수값은 아래입니다.

- `SPOTIFY_OAUTH_ENABLED`
- `SPOTIFY_CLIENT_ID`
- `SPOTIFY_REDIRECT_URI`

추가값은 현재와 다음 단계에 사용됩니다.

- `SPOTIFY_CLIENT_SECRET`
  현재 PKCE authorization-code exchange에는 직접 쓰지 않지만, 이후 refresh 흐름과 서버 비밀값 관리 기준을 위해 같이 보관합니다.
- `SPOTIFY_AUTHORIZATION_URI`
- `SPOTIFY_TOKEN_URI`
- `SPOTIFY_API_BASE_URI`

## 실행 방법

Spotify 설정이 들어간 API 실행:

```bash
cd /Users/woosungjo/music-space/my-forever-music
bash infra/scripts/run-api-with-spotify-env.sh
```

직접 실행:

```bash
cd /Users/woosungjo/music-space/my-forever-music/services/api
set -a
source .env.local
set +a
export PATH="/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
./gradlew bootRun
```

## 테스트 전 확인사항

- Spotify Dashboard Redirect URI에 `https://imapplepie20.tplinkdns.com/platforms/oauth/callback` 가 등록되어 있어야 합니다.
- 브라우저에서 `https://imapplepie20.tplinkdns.com/platforms/oauth/callback` 경로로 실제 접근 가능한 HTTPS 서비스가 떠 있어야 합니다.
- 단순히 `http://imapplepie20.tplinkdns.com:5173/` 만 열려 있는 상태면 Spotify OAuth callback은 동작하지 않습니다.
- 외부 도메인용 Spotify OAuth는 `HTTPS`가 필요합니다.

Nginx와 Certbot 기준 실제 배치 절차는 [HTTPS_DOMAIN_DEV_SETUP.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/HTTPS_DOMAIN_DEV_SETUP.md) 를 본다.

## 현재 구현 상태

- `/api/v1/platforms/oauth/start` 는 Spotify PKCE authorize URL을 생성합니다.
- `/platforms/oauth/callback` 은 프론트에서 `code` 와 `state` 를 읽어 `/api/v1/platforms/oauth/complete` 로 넘깁니다.
- `/api/v1/platforms/oauth/complete` 는 Spotify token endpoint와 authorization code를 교환합니다.
- 이후 `PMS import` 는 실제 Spotify playlist/provider 흐름을 사용합니다.

## 다음 연결 지점

1. HTTPS 도메인에서 실제 callback 페이지 서빙 확인
2. Spotify OAuth live 테스트
3. Spotify access token refresh 구현

## 공식 참고

- Spotify PKCE flow:
  https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow
- Spotify insecure redirect URI migration:
  https://developer.spotify.com/documentation/web-api/tutorials/migration-insecure-redirect-uri
- Spotify OAuth migration notice:
  https://developer.spotify.com/blog/2025-10-14-reminder-oauth-migration-27-nov-2025
