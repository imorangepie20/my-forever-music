# Project Guide

작성일: `2026-04-29`

이 문서는 `my-forever-music`의 가장 중요한 진입 문서입니다.  
새로운 세션, 새로운 작업자, 새로운 AI 에이전트는 이 문서를 먼저 읽고 현재 프로젝트 방향을 파악해야 합니다.

## 1. 프로젝트 한 줄 요약

`my-forever-music`은 기존 MusicSpace 개념을 바탕으로 다시 만드는 프로젝트이며, 먼저 웹앱을 완성한 뒤 같은 도메인과 UI 자산을 재사용해 Windows 데스크탑 앱까지 확장합니다.

## 2. 현재 확정된 큰 방향

- 프론트엔드는 `React + TypeScript + Vite`
- 메인 API는 `Spring Boot 3.5.x + Java 21 + Gradle`
- AI/추천 서비스는 `FastAPI`
- 데이터베이스는 `PostgreSQL`
- 마이그레이션은 `Flyway`
- Windows 데스크탑 앱은 후속 단계에서 `Tauri 2`
- API 계약 문서 기준은 `OpenAPI`

## 3. 왜 이렇게 정했는가

- 레거시 분석 결과, 도메인 구조의 강점은 `PMS / EMS / GMS` 흐름에 있었음
- 메인 백엔드는 Node보다 Spring 구조가 현재 목표와 더 자연스럽게 맞음
- 웹과 데스크탑이 같은 API와 도메인 모델을 공유해야 함
- 추천/AI는 메인 API와 분리된 별도 서비스가 유지보수에 유리함

자세한 근거는 아래 문서를 본다.

- [TECH_STACK.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/TECH_STACK.md)
- [ADR-001-backend-stack.md](/Users/woosungjo/music-space/my-forever-music/docs/decisions/ADR-001-backend-stack.md)
- [DESKTOP_APP_STRATEGY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/DESKTOP_APP_STRATEGY.md)
- [UBUNTU_SERVER_RUNBOOK.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/UBUNTU_SERVER_RUNBOOK.md)
- [UBUNTU_SERVER_SETUP_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/UBUNTU_SERVER_SETUP_GUIDE.md)

## 4. 현재 소스 구조 이해

```text
apps/
  web/         -> 브라우저용 사용자 앱
  desktop/     -> 향후 Tauri 기반 Windows 앱

services/
  api/         -> Spring Boot 메인 API
  ai/          -> FastAPI 추천/AI 서비스

packages/
  shared-types/
  shared-utils/
  design-tokens/

docs/
  architecture/
  api/
  product/
  diagrams/
  decisions/
```

## 5. 각 영역의 책임

- `apps/web`: PMS / EMS / GMS 중심 사용자 경험
- `apps/desktop`: 웹 UI와 공통 로직을 재사용하는 Windows 셸
- `services/api`: 인증, 사용자, 트랙, 플레이리스트, 플랫폼 연동, 추천 오케스트레이션
- `services/ai`: 모델 추론, 임베딩, 추천 계산, AI 보강
- `packages/shared-types`: 프론트와 연계되는 도메인 타입
- `packages/shared-utils`: 공통 헬퍼와 클라이언트 유틸
- `packages/design-tokens`: 재사용 UI 토큰
- `infra/nginx`: 웹 정적 서빙과 API/AI 리버스 프록시

## 6. 새 세션이 시작되면 먼저 볼 문서

아래 순서대로 보면 현재 문맥을 가장 빨리 따라올 수 있습니다.

1. [README.md](/Users/woosungjo/music-space/my-forever-music/README.md)
2. [PROJECT_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_GUIDE.md)
3. [TECH_STACK.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/TECH_STACK.md)
4. [DESKTOP_APP_STRATEGY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/DESKTOP_APP_STRATEGY.md)
5. [ADR-001-backend-stack.md](/Users/woosungjo/music-space/my-forever-music/docs/decisions/ADR-001-backend-stack.md)
6. [services/api/README.md](/Users/woosungjo/music-space/my-forever-music/services/api/README.md)
7. [services/ai/README.md](/Users/woosungjo/music-space/my-forever-music/services/ai/README.md)
8. [UBUNTU_SERVER_RUNBOOK.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/UBUNTU_SERVER_RUNBOOK.md)
9. [UBUNTU_SERVER_SETUP_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/UBUNTU_SERVER_SETUP_GUIDE.md)

## 7. 앞으로 문서를 갱신하는 규칙

- 기술 스택이 바뀌면 `TECH_STACK.md`를 먼저 수정
- 구조 결정이 바뀌면 `ADR`를 추가하거나 수정
- 새 세션이 꼭 알아야 할 공통 방향이 바뀌면 이 `PROJECT_GUIDE.md`를 갱신
- 루트 구조가 바뀌면 `README.md`도 함께 수정

## 8. 지금 시점의 우선순위

1. `services/api` 로컬 실행 환경(Java/Gradle/PostgreSQL) 연결
2. `apps/web` 템플릿을 PMS / EMS / GMS 중심 구조로 축소
3. 공통 타입과 API 계약 체계 정리
4. DB 스키마와 Flyway 마이그레이션 초안 작성
5. 인증과 사용자 설정부터 API 구현 시작

## 9. 참고 메모

- `docs/streaming-platforms-api/*`는 외부 음악 플랫폼 연동 참고 자료다
- 이 문서들은 제품 방향 문서가 아니라 통합 참고 문서이므로, 핵심 구조 결정은 이 가이드와 ADR 문서를 우선한다

## 10. 세션용 한 줄 지시문

새 세션은 추측으로 구조를 바꾸지 말고, 먼저 `docs/PROJECT_GUIDE.md`와 `AGENTS.md`를 읽은 뒤 현재 결정 사항에 맞춰 작업을 시작한다.
