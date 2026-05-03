# HTTPS Domain Dev Setup

작성일: `2026-05-03`

이 문서는 `imapplepie20.tplinkdns.com` 도메인으로 `my-forever-music` 개발 서버를 HTTPS로 노출해 Spotify OAuth callback까지 통과시키는 절차를 정리합니다.

## 목표

- `http://imapplepie20.tplinkdns.com:5173/` 직접 접속 대신
- `https://imapplepie20.tplinkdns.com/` 를 고정 진입점으로 사용
- 프론트는 `Vite 5173`
- API는 `Spring Boot 8081`
- AI는 `FastAPI 8000`
- Nginx가 `80/443` 을 받아 HTTPS와 reverse proxy를 담당

## 현재 프로젝트 기준 파일

- HTTP bootstrap 설정:
  [ubuntu.server.dev.conf](/Users/woosungjo/music-space/my-forever-music/infra/nginx/ubuntu.server.dev.conf)
- HTTPS 최종 설정:
  [ubuntu.server.dev.https.conf](/Users/woosungjo/music-space/my-forever-music/infra/nginx/ubuntu.server.dev.https.conf)
- Spotify OAuth 기준:
  [SPOTIFY_OAUTH_SETUP.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/SPOTIFY_OAUTH_SETUP.md)
- 웹 실행 스크립트:
  [run-web-with-domain-env.sh](/Users/woosungjo/music-space/my-forever-music/infra/scripts/run-web-with-domain-env.sh)
- API 실행 스크립트:
  [run-api-with-spotify-env.sh](/Users/woosungjo/music-space/my-forever-music/infra/scripts/run-api-with-spotify-env.sh)

## 1. DNS 와 포트 확인

- `imapplepie20.tplinkdns.com` 이 개발 서버 공인 IP를 가리켜야 합니다.
- 외부에서 `80/tcp`, `443/tcp` 가 열려 있어야 합니다.
- Spotify OAuth용 callback은 외부에서 `https://imapplepie20.tplinkdns.com/platforms/oauth/callback` 로 접근 가능해야 합니다.

## 2. 웹과 API 실행

웹 실행:

```bash
cd /Users/woosungjo/music-space/my-forever-music
bash infra/scripts/run-web-with-domain-env.sh
```

API 실행:

```bash
cd /Users/woosungjo/music-space/my-forever-music
bash infra/scripts/run-api-with-spotify-env.sh
```

AI 실행:

```bash
cd /Users/woosungjo/music-space/my-forever-music/services/ai
source .venv/bin/activate
AI_ROOT_PATH=/ai uvicorn app.main:app --host 0.0.0.0 --port 8000
```

현재 웹 `.env.local` 은 아래 기준입니다.

```text
VITE_PUBLIC_HOST=imapplepie20.tplinkdns.com
VITE_HMR_PROTOCOL=wss
VITE_HMR_CLIENT_PORT=443
```

즉 Nginx HTTPS reverse proxy 뒤에서도 Vite HMR websocket이 `wss://imapplepie20.tplinkdns.com` 기준으로 붙도록 맞춰져 있습니다.

## 3. HTTP bootstrap Nginx 적용

먼저 HTTP만 살아 있는 상태로 Nginx를 붙입니다.

예시:

```bash
sudo cp /Users/woosungjo/music-space/my-forever-music/infra/nginx/ubuntu.server.dev.conf /etc/nginx/nginx.conf
sudo mkdir -p /var/www/certbot
sudo nginx -t
sudo systemctl reload nginx
```

이 상태에서 아래 주소가 열려야 합니다.

```text
http://imapplepie20.tplinkdns.com/
```

## 4. Certbot 으로 인증서 발급

Certbot 공식 권장 흐름은 `snap` 설치 후 `certbot --nginx` 또는 `certonly --nginx` 입니다.

```bash
sudo apt-get remove certbot || true
sudo snap install --classic certbot
sudo ln -s /snap/bin/certbot /usr/local/bin/certbot
sudo certbot --nginx -d imapplepie20.tplinkdns.com
sudo certbot renew --dry-run
```

`certbot --nginx` 는 현재 Nginx 설정을 읽어 HTTPS 인증서 발급과 적용을 함께 시도합니다.

## 5. Repo 기준 HTTPS 설정으로 고정하고 싶을 때

인증서가 발급된 뒤 repo의 HTTPS 설정으로 맞추려면 아래 파일을 사용합니다.

- [ubuntu.server.dev.https.conf](/Users/woosungjo/music-space/my-forever-music/infra/nginx/ubuntu.server.dev.https.conf)

적용 예시:

```bash
sudo cp /Users/woosungjo/music-space/my-forever-music/infra/nginx/ubuntu.server.dev.https.conf /etc/nginx/nginx.conf
sudo nginx -t
sudo systemctl reload nginx
```

이 설정은 아래 기준입니다.

- `80` -> `443` redirect
- `443` -> `Vite 5173`, `API 8081`, `AI 8000`
- Let’s Encrypt 인증서 경로:
  - `/etc/letsencrypt/live/imapplepie20.tplinkdns.com/fullchain.pem`
  - `/etc/letsencrypt/live/imapplepie20.tplinkdns.com/privkey.pem`

## 6. Spotify OAuth 확인

Spotify Dashboard Redirect URI:

```text
https://imapplepie20.tplinkdns.com/platforms/oauth/callback
```

브라우저 테스트 순서:

1. `https://imapplepie20.tplinkdns.com/signup`
2. 계정 생성
3. `/platforms` 에서 Spotify OAuth 시작
4. Spotify 승인 후 `/platforms/oauth/callback`
5. `/pms` 에서 실제 Spotify playlist import 확인

## 7. 현재 구현 메모

- API는 현재 `local` 프로필 기본 포트 `8081` 기준입니다.
- HTTP bootstrap 설정도 이 포트 기준으로 맞춰져 있습니다.
- `Vite` 는 domain HMR 환경 변수를 읽도록 업데이트되었습니다.
- `services/api/.env.local` 과 `apps/web/.env.local` 은 현재 Git ignore 대상입니다.

## 다음 연결 지점

1. HTTPS domain 경로에서 실제 Spotify OAuth live 테스트
2. access token refresh 구현
3. 운영용 정적 프론트 배포와 production Nginx 분리

## 공식 참고

- Certbot instructions for Nginx:
  https://certbot.eff.org/instructions?ws=nginx
- Spotify PKCE flow:
  https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow
- Spotify insecure redirect URI migration:
  https://developer.spotify.com/documentation/web-api/tutorials/migration-insecure-redirect-uri
