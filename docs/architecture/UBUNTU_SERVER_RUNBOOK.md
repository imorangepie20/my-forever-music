# Ubuntu Server Runbook

작성일: `2026-04-29`

이 문서는 `my-forever-music`을 Ubuntu 서버에서 개발 및 서비스할 때의 기준 가이드입니다.

현재 프로젝트의 1차 구현과 시험 서비스 환경은 `MacBook 로컬`입니다. 따라서 이 문서는 `지금 당장 주 개발 환경`이 아니라, 로컬 검증 후 Ubuntu로 승격할 때 따르는 기준 문서로 봅니다.

## 기본 전략

현재 프로젝트는 아래 두 단계를 기준으로 운영합니다.

1. MacBook 로컬 개발/시험 단계
   - `apps/web`: MacBook에서 `Vite`
   - `services/api`: MacBook에서 `Spring Boot`
   - `services/ai`: MacBook에서 `FastAPI`
   - 필요 시 로컬 Docker로 `PostgreSQL`, `Redis` 사용

2. Ubuntu 이전 후 개발/서비스 단계
   - `apps/web`: 호스트에서 `Vite`
   - `services/api`: 호스트에서 `Spring Boot`
   - `services/ai`: 호스트 또는 컨테이너에서 `FastAPI`
   - `Nginx`: Ubuntu 서버에서 리버스 프록시

3. 서비스 단계
   - `Nginx + API + AI + PostgreSQL + Redis`를 Docker Compose 또는 systemd 조합으로 운영
   - 프론트는 빌드 결과물을 Nginx가 직접 서빙

## 권장 Ubuntu 기준

- Ubuntu `22.04 LTS` 또는 `24.04 LTS`
- `Docker Engine` 공식 apt 저장소 사용
- `Nginx` 공식 저장소 또는 Ubuntu 패키지 사용
- `Java 21`은 `Eclipse Temurin`

이 내용은 `2026-04-29` 기준 공식 문서를 참고한 권장안입니다.

## 가장 추천하는 개발 방식

처음에는 모든 것을 Docker로 묶기보다 아래처럼 시작하는 것이 가장 단순합니다.

- Nginx: Ubuntu 서버에 직접 설치
- Web: `pnpm dev -- --host 0.0.0.0`
- API: `./gradlew bootRun`
- AI: `AI_ROOT_PATH=/ai uvicorn app.main:app --host 0.0.0.0 --port 8000`
- DB/Redis만 Docker Compose 사용 가능

이 방식이면 코드 수정과 로그 확인이 빠르고, Nginx는 고정 진입점 역할만 하면 됩니다.

## Nginx 기준

Ubuntu 서버 개발용 Nginx 설정은 아래 파일을 사용합니다.

- [ubuntu.server.dev.conf](/Users/woosungjo/music-space/my-forever-music/infra/nginx/ubuntu.server.dev.conf)
- [ubuntu.server.dev.https.conf](/Users/woosungjo/music-space/my-forever-music/infra/nginx/ubuntu.server.dev.https.conf)

경로 라우팅은 다음 기준입니다.

- `/` -> `apps/web` Vite 서버
- `/api/` -> `services/api` 기본 `8081`
- `/actuator/` -> `services/api`
- `/docs`, `/openapi` -> `services/api`
- `/ai/` -> `services/ai`

Spotify OAuth callback까지 포함한 실제 HTTPS 도메인 절차는 [HTTPS_DOMAIN_DEV_SETUP.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/HTTPS_DOMAIN_DEV_SETUP.md) 를 본다.

## Docker Compose 기준

Ubuntu 서버용 템플릿은 아래 파일을 사용합니다.

- [docker-compose.ubuntu-dev.yml](/Users/woosungjo/music-space/my-forever-music/infra/docker/docker-compose.ubuntu-dev.yml)
- [env.ubuntu.example](/Users/woosungjo/music-space/my-forever-music/infra/docker/env.ubuntu.example)

현재 이 Compose 파일은 `PostgreSQL`과 `Redis` 중심 템플릿입니다.

두 포트는 개발 단계에서 외부 공개를 줄이기 위해 `127.0.0.1` 바인딩을 기준으로 잡았습니다.

개발 단계에서는:

- `Nginx`는 Ubuntu 호스트에 직접 설치
- `web`, `api`는 호스트 프로세스로 실행
- `DB`, `Redis`만 Docker Compose 사용

## 설치 우선순위

1. Docker Engine
2. Nginx
3. Java 21
4. Node.js LTS
5. Python 3.12 계열

## 운영 전환 전 체크

- `apps/web`를 정적 빌드 기준으로 정리
- `services/api`에 Gradle wrapper 추가
- `services/ai` 추천/모델 라우트와 내부 서비스 계층 확장
- PostgreSQL 스키마와 Flyway 마이그레이션 준비
- Nginx HTTPS 설정과 도메인 연결

## 공식 참고

- Docker Ubuntu install: `https://docs.docker.com/engine/install/ubuntu/`
- Nginx Linux packages: `https://nginx.org/en/linux_packages.html`
- Eclipse Temurin install: `https://adoptium.net/installation/linux/`

자세한 설치 순서는 [UBUNTU_SERVER_SETUP_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/UBUNTU_SERVER_SETUP_GUIDE.md) 를 본다.
