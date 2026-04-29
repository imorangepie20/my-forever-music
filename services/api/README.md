# services/api

Spring Boot 메인 API 서비스 폴더입니다.

## 목표

- 인증, 사용자 설정, 플레이리스트, 트랙, 플랫폼 연동 담당
- PMS / EMS / GMS 도메인 오케스트레이션 담당
- `apps/web`, `apps/desktop`가 공통으로 사용하는 백엔드 API 제공

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

## 현재 스캐폴드에 포함된 것

- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`
- Spring Boot 메인 애플리케이션 클래스
- 기본 보안 설정
- OpenAPI 설정
- 샘플 엔드포인트: `/api/v1/system/info`
- GMS AI preview 브리지 엔드포인트: `POST /api/v1/gms/recommendations/preview`
- Actuator 설정: `/actuator/health`
- Swagger UI 경로: `/docs`
- OpenAPI 문서 경로: `/openapi`

## 실행 전제

- `Java 21`
- `Gradle 8.14+` 또는 Gradle wrapper
- `PostgreSQL`

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

## 실행 예시

```bash
./gradlew bootRun
```

AI 서비스와 함께 로컬 실행 예시:

```bash
AI_SERVICE_BASE_URL=http://localhost:8000 ./gradlew bootRun
```

예시 호출:

```bash
curl -X POST http://127.0.0.1:8080/api/v1/gms/recommendations/preview \
  -H 'Content-Type: application/json' \
  -d '{
    "mode": "gms",
    "mood": "upbeat",
    "limit": 3,
    "seed_track_ids": ["track-alpha", "track-beta"]
  }'
```

이 엔드포인트는 현재 `GMS` preview 브리지이므로 `mode`는 생략하거나 `gms`로 고정해 사용하는 것을 기준으로 한다.

## Wrapper 상태

- 현재 `gradle-wrapper.properties`와 `gradlew` 스크립트는 들어가 있음
- 다만 `gradle-wrapper.jar`는 아직 커밋되지 않음
- 그래서 첫 실행 시 wrapper jar가 없으면 `system Gradle`로 자동 폴백함
- Ubuntu 서버에서 Java/Gradle 설치 후 아래를 한 번 실행하면 공식 wrapper 자산을 고정하기 쉬움

```bash
./gradlew wrapper
```
