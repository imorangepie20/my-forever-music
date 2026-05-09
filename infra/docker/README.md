# infra/docker

개발용 컨테이너 실행 템플릿 모음입니다.

## 파일

- [docker-compose.local-db.yml](/Users/woosungjo/music-space/my-forever-music/infra/docker/docker-compose.local-db.yml): 로컬 개발용 `PostgreSQL + Redis`
- [env.local.example](/Users/woosungjo/music-space/my-forever-music/infra/docker/env.local.example): 로컬 개발용 환경 변수 예시
- [docker-compose.ubuntu-dev.yml](/Users/woosungjo/music-space/my-forever-music/infra/docker/docker-compose.ubuntu-dev.yml): Ubuntu 서버 개발용 `PostgreSQL + Redis`
- [env.ubuntu.example](/Users/woosungjo/music-space/my-forever-music/infra/docker/env.ubuntu.example): Ubuntu 서버 개발용 환경 변수 예시

## 로컬 DB 기동 예시

전체 MacBook 시험 스택을 재시작할 때는 개별 compose 명령보다 아래 스크립트를 우선 사용한다. 이 스크립트는 PostgreSQL/Redis, HTTPS 도메인 프록시, FastAPI AI, Spring Boot API, Vite 웹 서버를 같은 순서로 다시 올린다.

```bash
cd /Users/woosungjo/music-space/my-forever-music
./infra/scripts/restart-macbook-stack.sh
```

옵션:

```bash
./infra/scripts/restart-macbook-stack.sh --tail
```

DB/Redis만 직접 기동하려면:

```bash
cd /Users/woosungjo/music-space/my-forever-music/infra/docker
docker compose -f docker-compose.local-db.yml up -d
```

macOS에서는 위 명령 전 `Docker Desktop` 또는 Docker daemon이 먼저 실행 중이어야 한다.

### 포트 설정

- PostgreSQL: `127.0.0.1:5433` → 컨테이너 내 `5432`
- Redis: `127.0.0.1:6379` → 컨테이너 내 `6379`

> **참고**: 로컬 개발 환경에서 다른 서비스와의 포트 충돌을 방지하기 위해 PostgreSQL은 호스트 포트 `5433`을 사용합니다.

### 컨테이너 상태 확인

```bash
docker compose -f docker-compose.local-db.yml ps
```

### 컨테이너 중지

```bash
docker compose -f docker-compose.local-db.yml down
```

### 데이터베이스 직접 접속

```bash
docker exec -it my-forever-music-local-postgres psql -U postgres -d my_forever_music
```

이후 API는 아래처럼 DB 프로필로 실행하면 된다.

```bash
cd /Users/woosungjo/music-space/my-forever-music/services/api
SPRING_PROFILES_ACTIVE=database DB_PORT=5433 AI_SERVICE_BASE_URL=http://localhost:8000 ./gradlew bootRun
```
