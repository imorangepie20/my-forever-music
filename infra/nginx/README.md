# infra/nginx

`my-forever-music`의 웹서버 및 리버스 프록시 설정 디렉토리입니다.

## 현재 권장 구조

- 로컬 개발: `Vite dev server` + `Spring Boot` + `FastAPI`
- 운영 배포: `Nginx` + 정적 프론트엔드 + `Spring Boot` + `FastAPI`

## 포함된 설정 파일

- `local.dev.conf`
  - 로컬 개발용 프록시
  - `/` -> Vite
  - `/api`, `/actuator`, `/docs`, `/openapi` -> Spring Boot
  - `/ai` -> FastAPI

- `ubuntu.server.dev.conf`
  - Ubuntu 서버에서 호스트 프로세스로 개발할 때 사용
  - `/` -> `127.0.0.1:5173`
  - `/api`, `/actuator`, `/docs`, `/openapi` -> `127.0.0.1:8081`
  - `/ai` -> `127.0.0.1:8000`

- `ubuntu.server.dev.https.conf`
  - Ubuntu 서버에서 HTTPS domain 개발용으로 사용
  - `80` -> `443` redirect
  - `443` 에서 `Vite 5173`, `Spring Boot 8081`, `FastAPI 8000` 으로 reverse proxy
  - `imapplepie20.tplinkdns.com` 과 Let’s Encrypt 경로 기준 예시 포함

- `production.conf`
  - 운영 배포용 설정
  - Nginx가 `apps/web` 빌드 결과물을 직접 서빙
  - API와 AI 경로는 각 백엔드로 프록시

## 기본 경로 규칙

- `/` -> 프론트엔드
- `/api/` -> `services/api`
- `/actuator/` -> `services/api`
- `/docs`, `/openapi` -> `services/api`
- `/ai/` -> `services/ai`

## 주의

- `local.dev.conf`는 Nginx가 호스트 머신의 개발 서버에 붙는 구성을 가정합니다
- Docker에서 로컬 개발 서버를 바라볼 때는 `host.docker.internal`이 필요할 수 있습니다
- Ubuntu 서버에서 직접 개발 서버를 띄우는 경우 `ubuntu.server.dev.conf`를 우선 사용합니다
- Spotify OAuth 같은 외부 OAuth callback 테스트는 `ubuntu.server.dev.https.conf` 또는 동등한 HTTPS 설정이 필요합니다
- `services/ai`의 FastAPI 문서를 `/ai/docs`로 정상 노출하려면 앱 실행 시 `AI_ROOT_PATH=/ai`를 주는 것을 권장합니다
- `production.conf`의 `root` 경로와 upstream 이름은 실제 배포 구조에 맞게 조정해야 합니다
