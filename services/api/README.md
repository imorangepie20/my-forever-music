# services/api

Spring Boot 메인 API 서비스 폴더입니다.

## 목표

- 인증, 사용자 설정, 플레이리스트, 트랙, 플랫폼 연동 담당
- PMS / EMS / GMS 도메인 오케스트레이션 담당
- `apps/web`, `apps/desktop`가 공통으로 사용하는 백엔드 API 제공
- 사용자가 구독 중인 스트리밍 플랫폼과 연결하고 플레이리스트를 PMS로 적재
- EMS 수집 데이터와 GMS 평가 결과가 다시 PMS 학습 데이터로 이어지는 환류 담당

## 권장 스택

- `Spring Boot 3.5.x`
- `Java 21`
- `Gradle`
- `Spring Web`
- `Spring Security`
- `Spring Data JPA`
- `Bean Validation`
- `Actuator`
- `Flyway`

## 권장 구조

```text
services/api/
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
│       ├── java/
│       └── resources/
└── README.md
```

## 구현 가이드

1. API 계약은 OpenAPI 기준으로 문서화
2. 도메인은 `auth`, `user`, `pms`, `ems`, `gms`, `platform` 기준으로 분리
3. 외부 AI 호출은 직접 모델을 넣지 말고 `services/ai` 연동 계층으로 분리
4. 데스크탑 앱을 고려해 세션보다 토큰/클라이언트 독립 구조를 우선 검토
5. 플랫폼 원본 데이터와 보강된 오디오 특성 데이터는 분리해 저장하는 방향을 우선 검토

## 현재 스캐폴드에 포함된 것

- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`
- Spring Boot 메인 애플리케이션 클래스
- 기본 보안 설정
- OpenAPI 설정
- 샘플 엔드포인트: `/api/v1/system/info`
- 회원가입 엔드포인트: `POST /api/v1/auth/register`
- 플랫폼 카탈로그 엔드포인트: `GET /api/v1/platforms/catalog`
- 플랫폼 연결 bootstrap 엔드포인트: `GET /api/v1/platforms/connections/bootstrap`
- 플랫폼 연결 명령 엔드포인트: `POST /api/v1/platforms/connections/connect`, `POST /api/v1/platforms/connections/disconnect`
- 플랫폼 OAuth 엔드포인트: `POST /api/v1/platforms/oauth/start`, `POST /api/v1/platforms/oauth/complete`
- PMS import bootstrap 엔드포인트: `GET /api/v1/pms/import/bootstrap`
- PMS import 명령 엔드포인트: `POST /api/v1/pms/import/playlists`
- PMS workspace bootstrap 엔드포인트: `GET /api/v1/pms/workspace/bootstrap`
- EMS workspace analysis 엔드포인트: `POST /api/v1/ems/workspace/analysis`
- GMS AI preview 브리지 엔드포인트: `POST /api/v1/gms/recommendations/preview`
- PMS bootstrap용 JPA 엔터티 / 리포지토리 / Flyway 마이그레이션
- Actuator 설정: `/actuator/health`
- Swagger UI 경로: `/docs`
- OpenAPI 문서 경로: `/openapi`

이 스캐폴드는 아직 핵심 서비스의 전체 구현이 아니라 `PMS bootstrap -> EMS analysis -> GMS preview` 최소 검증 버전이다. 장기적으로는 플랫폼 OAuth, 플레이리스트 동기화, 행동 이벤트 적재, GMS 평가 저장까지 확장한다.

현재는 여기에 `회원가입 -> 기본 플랫폼 선택 -> 플랫폼 연결 상태 조회 -> sandbox/Spotify OAuth 승인/callback -> PMS playlist import -> EMS 다음 단계 안내`까지 추가되어, 서비스 구현을 온보딩부터 순차적으로 확장할 수 있는 상태다.

현재 `pms_track`는 기본 메타데이터만이 아니라 `Spotify 오디오 특성 전체 스냅샷`을 저장할 수 있도록 확장되어 있다. 따라서 향후 플랫폼 playlist import는 트랙 저장 전에 오디오 특성 채움 과정을 반드시 거치는 것을 기본 전제로 한다.

또한 현재는 `platform credential store`와 `platform playlist provider` 계층이 추가되어, sandbox와 실제 외부 플랫폼 연동을 같은 import 흐름 위에서 모드별로 처리할 수 있다.

추가로 `platform authorization code exchange client` 계층도 들어가 있어서, Spotify PKCE draft가 켜져 있으면 callback의 authorization code를 실제 token endpoint와 교환하고 그 결과를 credential로 저장한다.

현재 `spotify` provider는 실제 사용자 token으로 `GET /me/playlists`, `GET /playlists/{playlist_id}/items`를 호출하고, 트랙별 오디오 특성은 공식 `GET /audio-features`를 우선 시도한 뒤 실패하거나 누락된 항목은 `fallback_generated` 완전 스냅샷으로 보강한다.

상세 저장 기준은 [PMS_TRACK_AUDIO_FEATURE_STORAGE.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_TRACK_AUDIO_FEATURE_STORAGE.md) 를 본다.

API 계약 문서 인덱스는 [docs/api/README.md](/Users/woosungjo/music-space/my-forever-music/docs/api/README.md) 를 먼저 본다.

## 실행 전제

- `Java 21`
- `Gradle wrapper`
- 첫 wrapper 생성이 필요한 경우에만 `Gradle 8.14+`

현재 `local` 프로필은 브라우저 확인과 API 연결 점검을 빠르게 할 수 있도록 `DataSource`, `JPA`, `Flyway` 자동 구성을 잠시 제외한다.  
즉, 현재 스캐폴드 상태에서는 `PostgreSQL` 없이도 `./gradlew bootRun`이 가능하다.

또한 macOS 로컬 개발에서는 `8080` 포트가 Docker나 다른 앱과 자주 충돌하므로, `local` 프로필 기본 포트는 `8081`을 사용한다. 운영/서버 기준 기본 포트는 여전히 `8080`이다.

이 상태에서도 `EMS workspace analysis`는 동작한다. 다만 `local`에서는 입력 텍스트 시드 중심으로 분석하고, `database` 프로필에서는 `PMS` 카탈로그 seed track도 함께 반영한다.

반대로 `database` 같은 DB 활성 프로필로 실행하면 Flyway가 `pms_*` 테이블을 만들고 demo bootstrap 데이터를 적재한다.

## 참고 환경 변수

- `API_PORT`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `APP_VERSION`
- `AI_SERVICE_BASE_URL`
- `AI_RECOMMENDATION_PREVIEW_PATH`
- `SPOTIFY_OAUTH_ENABLED`
- `SPOTIFY_CLIENT_ID`
- `SPOTIFY_CLIENT_SECRET`
- `SPOTIFY_REDIRECT_URI`
- `SPOTIFY_AUTHORIZATION_URI`
- `SPOTIFY_TOKEN_URI`
- `SPOTIFY_API_BASE_URI`

## 실행 예시

```bash
./gradlew bootRun
```

AI 서비스와 함께 로컬 실행 예시:

```bash
AI_SERVICE_BASE_URL=http://localhost:8000 ./gradlew bootRun
```

PostgreSQL을 붙여 PMS bootstrap을 DB 기반으로 확인하려면:

```bash
cd /Users/woosungjo/music-space/my-forever-music/infra/docker
cp env.local.example .env
docker compose -f docker-compose.local-db.yml up -d

cd /Users/woosungjo/music-space/my-forever-music/services/api
SPRING_PROFILES_ACTIVE=database \
DB_HOST=localhost \
DB_PORT=5432 \
DB_NAME=my_forever_music \
DB_USERNAME=postgres \
DB_PASSWORD=postgres \
AI_SERVICE_BASE_URL=http://localhost:8000 \
./gradlew bootRun
```

macOS에서는 `docker compose` 실행 전에 `Docker Desktop`이 떠 있어야 한다.

관련 파일:

- [docker-compose.local-db.yml](/Users/woosungjo/music-space/my-forever-music/infra/docker/docker-compose.local-db.yml)
- [env.local.example](/Users/woosungjo/music-space/my-forever-music/infra/docker/env.local.example)

Homebrew 기반 macOS에서 `java`가 바로 잡히지 않으면:

```bash
export PATH="/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
AI_SERVICE_BASE_URL=http://localhost:8000 ./gradlew bootRun
```

Spotify PKCE draft까지 같이 확인하려면:

```bash
export SPOTIFY_OAUTH_ENABLED=true
export SPOTIFY_CLIENT_ID=your_spotify_client_id
export SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
export SPOTIFY_REDIRECT_URI=https://your-domain.example.com/platforms/oauth/callback
AI_SERVICE_BASE_URL=http://localhost:8000 ./gradlew bootRun
```

이 설정이 있으면 `/api/v1/platforms/oauth/start`의 Spotify 응답은 internal sandbox approval 대신 external authorization URL을 내려준다. 이후 `/api/v1/pms/import/bootstrap`와 `/api/v1/pms/import/playlists`는 실제 Spotify playlist/provider 경로를 사용한다.

프로젝트에 맞춘 상세 설정과 실행 스크립트는 [SPOTIFY_OAUTH_SETUP.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/SPOTIFY_OAUTH_SETUP.md) 를 본다.

예시 호출:

```bash
curl -X POST http://127.0.0.1:8081/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "display_name": "Forever Listener",
    "email": "listener@example.com",
    "password": "music2026",
    "preferred_platform_id": "spotify",
    "marketing_opt_in": true,
    "accepted_terms": true,
    "accepted_privacy_policy": true
  }'
```

```bash
curl -X POST http://127.0.0.1:8081/api/v1/gms/recommendations/preview \
  -H 'Content-Type: application/json' \
  -d '{
    "mode": "gms",
    "mood": "upbeat",
    "limit": 3,
    "seed_track_ids": ["track-alpha", "track-beta"]
  }'
```

이 엔드포인트는 현재 `GMS` preview 브리지이므로 `mode`는 생략하거나 `gms`로 고정해 사용하는 것을 기준으로 한다.

PMS bootstrap은 현재 아래 세 모드로 동작한다.

- `imported-user`: 현재 사용자 기준 PMS import 결과
- `database`: Flyway로 적재된 `pms_playlist`, `pms_track`, `pms_playlist_track` 기반 bootstrap
- `local`: 정적 fallback bootstrap

## Wrapper 상태

- 현재 `gradle-wrapper.properties`, `gradlew`, `gradlew.bat`, `gradle-wrapper.jar`가 생성된 상태다
- 그래서 이후에는 `system Gradle` 없이도 wrapper 기준 실행이 가능하다
- 첫 설정을 다시 만들 때만 아래 명령이 필요하다

```bash
./gradlew wrapper
```
