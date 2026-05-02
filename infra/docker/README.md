# infra/docker

개발용 컨테이너 실행 템플릿 모음입니다.

## 파일

- [docker-compose.local-db.yml](/Users/woosungjo/music-space/my-forever-music/infra/docker/docker-compose.local-db.yml): 로컬 개발용 `PostgreSQL + Redis`
- [env.local.example](/Users/woosungjo/music-space/my-forever-music/infra/docker/env.local.example): 로컬 개발용 환경 변수 예시
- [docker-compose.ubuntu-dev.yml](/Users/woosungjo/music-space/my-forever-music/infra/docker/docker-compose.ubuntu-dev.yml): Ubuntu 서버 개발용 `PostgreSQL + Redis`
- [env.ubuntu.example](/Users/woosungjo/music-space/my-forever-music/infra/docker/env.ubuntu.example): Ubuntu 서버 개발용 환경 변수 예시

## 로컬 DB 기동 예시

```bash
cd /Users/woosungjo/music-space/my-forever-music/infra/docker
cp env.local.example .env
docker compose -f docker-compose.local-db.yml up -d
```

macOS에서는 위 명령 전 `Docker Desktop` 또는 Docker daemon이 먼저 실행 중이어야 한다.

이후 API는 아래처럼 DB 프로필로 실행하면 된다.

```bash
cd /Users/woosungjo/music-space/my-forever-music/services/api
SPRING_PROFILES_ACTIVE=database AI_SERVICE_BASE_URL=http://localhost:8000 ./gradlew bootRun
```
