# Database Setup Guide

작성일: `2026-05-05`

이 문서는 `my-forever-music` 프로젝트에서 실제 데이터베이스를 사용하여 개발하는 방법을 설명합니다.

## 개요

로컬 개발 환경에서는 Docker를 사용하여 PostgreSQL과 Redis를 실행하고, Spring Boot의 `database` 프로필을 통해 연결합니다.

## Spring Boot 프로필

### local 프로필 (기본값)

- **DataSource 비활성화**: DB 없이 메모리 상태로 실행
- **JPA 비활성화**: 영속성 계층 없이 동작
- **Flyway 비활성화**: 마이그레이션 실행하지 않음
- **용도**: 빠른 API 개발/테스트, 브라우저 연결 확인

### database 프로필

- **DataSource 활성화**: PostgreSQL 연결
- **JPA 활성화**: 영속성 계층 동작
- **Flyway 활성화**: 마이그레이션 자동 실행
- **ddl-auto: none**: Flyway로 스키마 관리, Hibernate 검증 건너뜀
- **용도**: 실제 데이터 영속성이 필요한 개발

## 빠른 시작

### 1. Docker Desktop 시작

macOS에서 Docker Desktop이 실행 중인지 확인합니다.

```bash
open -a Docker
```

### 2. PostgreSQL/Redis 컨테이너 시작

```bash
cd /Users/woosungjo/music-space/my-forever-music/infra/docker
docker compose -f docker-compose.local-db.yml up -d
```

### 3. 컨테이너 상태 확인

```bash
docker compose -f docker-compose.local-db.yml ps
```

정상 실행 시:
```
NAME                              STATUS                    PORTS
my-forever-music-local-postgres   Up (healthy)              127.0.0.1:5433->5432/tcp
my-forever-music-local-redis      Up (healthy)              6379/tcp
```

### 4. Spring Boot 실행 (database 프로필)

```bash
cd /Users/woosungjo/music-space/my-forever-music/services/api
SPRING_PROFILES_ACTIVE=database DB_PORT=5433 ./gradlew bootRun
```

## 포트 설정

| 서비스 | 호스트 포트 | 컨테이너 포트 | 비고 |
|--------|-------------|--------------|------|
| PostgreSQL | 5433 | 5432 | 다른 서비스와 충돌 방지 |
| Redis | 6379 | 6379 | 기본값 |
| Spring Boot API | 8081 | - | local/database 프로필 공통 |

## 환경 변수

### database 프로필 필수 변수

```bash
DB_PORT=5433                    # PostgreSQL 호스트 포트
DB_HOST=localhost               # PostgreSQL 호스트
DB_NAME=my_forever_music        # 데이터베이스 이름
DB_USERNAME=postgres            # 사용자명
DB_PASSWORD=postgres            # 비밀번호
```

### 선택 변수 (AI 서비스 연동)

```bash
AI_SERVICE_BASE_URL=http://localhost:8000
```

## Flyway 마이그레이션

### 현재 마이그레이션 목록

```
V1  : create pms bootstrap catalog
V2  : seed demo pms bootstrap catalog
V3  : add spotify audio features to pms track
V4  : create auth user account
V5  : create platform account connection
V6  : create platform authorization session
V7  : create platform account credential
V8  : extend platform authorization session for pkce
V9  : create pms import storage
V10 : add lastfm profile to auth user account
V11 : create lastfm scrobble storage
V12 : create pms user library storage
V13 : add rich media metadata to pms library
V14 : allow external oauth without internal approval code
V15 : create gms recommendation feedback
V16 : create pms personal playlist storage
```

### 마이그레이션 확인

```bash
docker exec my-forever-music-local-postgres psql -U postgres -d my_forever_music -c "SELECT version, description FROM flyway_schema_history ORDER BY installed_rank;"
```

## 데이터베이스 작업

### 직접 접속

```bash
docker exec -it my-forever-music-local-postgres psql -U postgres -d my_forever_music
```

### 테이블 목록 확인

```sql
\dt
```

### 데이터 조회 예시

```sql
-- 사용자 계정 조회
SELECT user_id, email, display_name, onboarding_stage FROM auth_user_account;

-- 플랫폼 연결 조회
SELECT * FROM platform_account_connection;

-- PMS 트랙 조회
SELECT track_id, title, artist_name FROM pms_track LIMIT 10;
```

### 데이터베이스 초기화

```bash
# 컨테이너 중지 및 볼륨 삭제
docker compose -f docker-compose.local-db.yml down -v

# 재시작 (데이터베이스 새로 생성)
docker compose -f docker-compose.local-db.yml up -d
```

## 문제 해결

### 포트 충돌

```
Error: bind: address already in use
```

**해결**: 포트를 사용 중인 프로세스 확인 후 중지

```bash
# 포트 사용 프로세스 확인
lsof -i :5433

# 또는 docker-compose 포트 변경
```

### Flyway 마이그레이션 실패

```
Error: Schema-validation: wrong column type
```

**해결**: `ddl-auto: none` 설정 확인 (application-database.yml)

### 컨테이너가 healthy 상태가 아님

```bash
# 컨테이너 로그 확인
docker logs my-forever-music-local-postgres
docker logs my-forever-music-local-redis

# 재시작
docker compose -f docker-compose.local-db.yml restart
```

## 관련 문서

- [services/api/README.md](/Users/woosungjo/music-space/my-forever-music/services/api/README.md)
- [infra/docker/README.md](/Users/woosungjo/music-space/my-forever-music/infra/docker/README.md)
- [PROJECT_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_GUIDE.md)
