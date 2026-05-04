# AGENTS.md

이 레포에서 작업을 시작하는 모든 세션은 먼저 아래 문서를 읽고 현재 방향을 맞춘다.

## 첫 진입 순서

1. [README.md](/Users/woosungjo/music-space/my-forever-music/README.md)
2. [docs/PROJECT_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_GUIDE.md)
3. [docs/PROJECT_KEY_SERVICE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_KEY_SERVICE.md)
4. [docs/product/USER_MUSIC_HOME_VISION.md](/Users/woosungjo/music-space/my-forever-music/docs/product/USER_MUSIC_HOME_VISION.md)
5. [docs/architecture/REAL_IMPLEMENTATION_POLICY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/REAL_IMPLEMENTATION_POLICY.md)
6. [docs/architecture/MACBOOK_LOCAL_FIRST_PLAN.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/MACBOOK_LOCAL_FIRST_PLAN.md)
7. [docs/architecture/TECH_STACK.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/TECH_STACK.md)
8. [docs/architecture/DESKTOP_APP_STRATEGY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/DESKTOP_APP_STRATEGY.md)
9. [docs/architecture/UBUNTU_SERVER_SETUP_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/UBUNTU_SERVER_SETUP_GUIDE.md)
10. [docs/decisions/ADR-001-backend-stack.md](/Users/woosungjo/music-space/my-forever-music/docs/decisions/ADR-001-backend-stack.md)
11. [services/api/README.md](/Users/woosungjo/music-space/my-forever-music/services/api/README.md)
12. [services/ai/README.md](/Users/woosungjo/music-space/my-forever-music/services/ai/README.md)

## 현재 확정 사항

- 메인 API는 `Node/NestJS`가 아니라 `Spring Boot 3.5.x + Java 21 + Gradle`
- 프론트는 `React + TypeScript + Vite`
- Windows 데스크탑 앱은 웹앱 이후 `Tauri 2`로 확장
- AI 서비스는 `FastAPI`
- DB는 `PostgreSQL`, 마이그레이션은 `Flyway`
- API 계약은 `OpenAPI`
- 제품 핵심 정의는 `docs/PROJECT_KEY_SERVICE.md`를 기준으로 해석한다
- 사용자 반복 사용 목적과 장기 제품 가치는 `docs/product/USER_MUSIC_HOME_VISION.md`를 함께 본다
- 사용자 플로우에는 mock data, sandbox provider, 임시 데이터 경로를 기본값으로 노출하지 않는다
- 현재 1차 구현/시험 서비스 환경은 `MacBook 로컬`이고, Ubuntu는 검증 후 이전 단계다

## 작업 원칙

- 구조나 스택을 바꾸면 관련 문서를 함께 업데이트한다
- 새 세션은 추측으로 진행하지 말고 `docs/PROJECT_GUIDE.md`를 기준으로 현재 상태를 확인한다
- 제품 목표를 해석할 때는 `docs/PROJECT_KEY_SERVICE.md`를 함께 확인하고, 현재 구현 상태와 목표 상태를 구분해서 문서화한다
- 새 기능은 가능한 한 특정 플랫폼에 종속시키지 말고 `PMS user library`와 사용자 소유 취향 모델로 흡수한다
- 아직 실제 provider가 완성되지 않은 기능은 사용자 선택지나 import 가능 상태로 노출하지 않는다
- 아키텍처 의사결정이 바뀌면 `docs/decisions`에 ADR을 추가한다
