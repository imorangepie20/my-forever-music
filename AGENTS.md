# AGENTS.md

이 레포에서 작업을 시작하는 모든 세션은 먼저 아래 문서를 읽고 현재 방향을 맞춘다.

## 첫 진입 순서

1. [README.md](/Users/woosungjo/music-space/my-forever-music/README.md)
2. [docs/PROJECT_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_GUIDE.md)
3. [docs/architecture/TECH_STACK.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/TECH_STACK.md)
4. [docs/architecture/DESKTOP_APP_STRATEGY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/DESKTOP_APP_STRATEGY.md)
5. [docs/architecture/UBUNTU_SERVER_SETUP_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/UBUNTU_SERVER_SETUP_GUIDE.md)
6. [docs/decisions/ADR-001-backend-stack.md](/Users/woosungjo/music-space/my-forever-music/docs/decisions/ADR-001-backend-stack.md)
7. [services/api/README.md](/Users/woosungjo/music-space/my-forever-music/services/api/README.md)
8. [services/ai/README.md](/Users/woosungjo/music-space/my-forever-music/services/ai/README.md)

## 현재 확정 사항

- 메인 API는 `Node/NestJS`가 아니라 `Spring Boot 3.5.x + Java 21 + Gradle`
- 프론트는 `React + TypeScript + Vite`
- Windows 데스크탑 앱은 웹앱 이후 `Tauri 2`로 확장
- AI 서비스는 `FastAPI`
- DB는 `PostgreSQL`, 마이그레이션은 `Flyway`
- API 계약은 `OpenAPI`

## 작업 원칙

- 구조나 스택을 바꾸면 관련 문서를 함께 업데이트한다
- 새 세션은 추측으로 진행하지 말고 `docs/PROJECT_GUIDE.md`를 기준으로 현재 상태를 확인한다
- 아키텍처 의사결정이 바뀌면 `docs/decisions`에 ADR을 추가한다
