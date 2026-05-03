# Project Guide

작성일: `2026-04-29`

이 문서는 `my-forever-music`의 가장 중요한 진입 문서입니다.  
새로운 세션, 새로운 작업자, 새로운 AI 에이전트는 이 문서를 먼저 읽고 현재 프로젝트 방향을 파악해야 합니다.

## 1. 프로젝트 한 줄 요약

`my-forever-music`은 기존 MusicSpace 개념을 바탕으로 다시 만드는 프로젝트이며, 먼저 웹앱을 완성한 뒤 같은 도메인과 UI 자산을 재사용해 Windows 데스크탑 앱까지 확장합니다.

핵심 서비스 원문 정의는 [PROJECT_KEY_SERVICE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_KEY_SERVICE.md) 를 기준으로 봅니다.

## 1-1. 서비스 핵심 정의

이 프로젝트가 만들려는 실제 서비스는 아래와 같습니다.

- 사용자가 구독 중인 스트리밍 플랫폼을 선택하고 계정을 연결한다
- 해당 플랫폼의 플레이리스트를 가져와 `PMS`에 저장한다
- 각 트랙의 오디오 특성을 우선 `Spotify` 기반으로 확보한다
- 오디오 특성을 직접 확보하지 못한 트랙은 웹 검색과 보강 로직으로 fallback 특성을 만든다
- 사용자 플레이리스트와 행동 데이터를 바탕으로 개인별 취향 모델을 점진적으로 학습한다
- `EMS`는 외부 플랫폼의 공개 플레이리스트와 트렌딩 트랙을 수집하는 외부 탐색 공간이다
- `GMS`는 사용자 모델이 통과시킨 추천 결과가 모이는 개인화 게이트웨이 공간이다
- 사용자가 `GMS` 결과를 평가하면 다시 `PMS` 학습 데이터로 환류된다
- 어느 페이지에서든 음악 재생이 가능하고 페이지 이동 사이에도 플레이어 상태가 유지된다

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
- 핵심 서비스 문서 기준으로도 `플랫폼 연동 / 취향 모델 / EMS-GMS 환류`는 장기적으로 분리된 서비스 구조가 더 적합함

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

- `apps/web`: PMS / EMS / GMS 중심 사용자 경험과 공통 플레이어 UI
- `apps/desktop`: 웹 UI와 공통 로직을 재사용하는 Windows 셸
- `services/api`: 인증, 사용자, 트랙, 플레이리스트, 플랫폼 연동, PMS / EMS / GMS 오케스트레이션
- `services/ai`: 오디오 특성 보강, 모델 추론, 추천 계산, AI 보강
- `packages/shared-types`: 프론트와 연계되는 도메인 타입
- `packages/shared-utils`: 공통 헬퍼와 클라이언트 유틸
- `packages/design-tokens`: 재사용 UI 토큰
- `infra/nginx`: 웹 정적 서빙과 API/AI 리버스 프록시

## 6. 새 세션이 시작되면 먼저 볼 문서

아래 순서대로 보면 현재 문맥을 가장 빨리 따라올 수 있습니다.

1. [README.md](/Users/woosungjo/music-space/my-forever-music/README.md)
2. [PROJECT_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_GUIDE.md)
3. [PROJECT_KEY_SERVICE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_KEY_SERVICE.md)
4. [TECH_STACK.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/TECH_STACK.md)
5. [DESKTOP_APP_STRATEGY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/DESKTOP_APP_STRATEGY.md)
6. [SPOTIFY_OAUTH_SETUP.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/SPOTIFY_OAUTH_SETUP.md)
7. [HTTPS_DOMAIN_DEV_SETUP.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/HTTPS_DOMAIN_DEV_SETUP.md)
8. [ADR-001-backend-stack.md](/Users/woosungjo/music-space/my-forever-music/docs/decisions/ADR-001-backend-stack.md)
9. [services/api/README.md](/Users/woosungjo/music-space/my-forever-music/services/api/README.md)
10. [docs/api/README.md](/Users/woosungjo/music-space/my-forever-music/docs/api/README.md)
11. [services/ai/README.md](/Users/woosungjo/music-space/my-forever-music/services/ai/README.md)
12. [UBUNTU_SERVER_RUNBOOK.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/UBUNTU_SERVER_RUNBOOK.md)
13. [UBUNTU_SERVER_SETUP_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/UBUNTU_SERVER_SETUP_GUIDE.md)

## 7. 앞으로 문서를 갱신하는 규칙

- 기술 스택이 바뀌면 `TECH_STACK.md`를 먼저 수정
- 구조 결정이 바뀌면 `ADR`를 추가하거나 수정
- 새 세션이 꼭 알아야 할 공통 방향이 바뀌면 이 `PROJECT_GUIDE.md`를 갱신
- 루트 구조가 바뀌면 `README.md`도 함께 수정

## 8. 지금 시점의 우선순위

1. Spotify access token refresh와 만료 복구 흐름 추가
2. PMS import 결과를 영속 사용자/플레이리스트 데이터 모델로 확장
3. Apple Music / TIDAL provider 확장 설계
4. 사용자 행동 데이터와 EMS 수집 데이터를 어떤 이벤트 모델로 저장할지 정의
5. 공통 타입과 API 계약 체계 정리

현재 참고 상태:

- `services/api`는 `Java 21 + Gradle wrapper` 기준으로 로컬 부팅 검증 완료
- `services/api`의 `local` 프로필은 현재 `PostgreSQL` 없이 부팅 가능
- `GET /api/v1/platforms/catalog` 응답 경로 추가 완료
- `POST /api/v1/auth/register` 회원가입 경로 추가 완료
- `GET/POST /api/v1/platforms/connections/*` 온보딩 연결 경로 추가 완료
- `POST /api/v1/platforms/oauth/*` sandbox/Spotify OAuth 시작/완료 경로 추가 완료
- `GET/POST /api/v1/pms/import/*` PMS playlist import 경로 추가 완료
- `services/api`에는 platform credential 저장소와 playlist provider 추상화가 추가되어 sandbox와 실제 Spotify import를 같은 흐름으로 처리함
- `services/api`는 실제 Spotify playlist listing/item import와 audio-features fallback 보강까지 반영됨
- `GET /api/v1/pms/workspace/bootstrap` 응답 검증 완료
- `POST /api/v1/ems/workspace/analysis` 응답 경로 추가 완료
- `POST /api/v1/gms/recommendations/preview`는 `services/ai`와의 브리지까지 검증 완료
- `services/api`에는 PMS bootstrap용 `Flyway + JPA` 최소 카탈로그 스키마와 demo 데이터가 추가됨
- `pms_track`는 Spotify 오디오 특성 전체 스냅샷 저장 구조로 확장됨
- PMS import 시 오디오 특성 전체 저장 기준 문서가 추가됨
- `docs/api/README.md`가 API 계약 문서의 공식 진입점으로 추가됨
- DB 활성 프로필에서는 PMS bootstrap이 실제 `pms_*` 테이블 기반으로 응답 가능
- `apps/web`에는 `/platforms` route가 추가되어 preferred PMS source platform을 선택할 수 있음
- `apps/web`에는 `/signup` route가 추가되어 회원가입과 기본 플랫폼 선택이 가능함
- `apps/web`는 가입 후 세션을 로컬에 저장하고 `/platforms`에서 sandbox 연결/해제와 Spotify OAuth redirect를 처리할 수 있음
- `apps/web`는 `/pms`에서 platform playlist import와 사용자별 workspace bootstrap을 사용할 수 있음
- 현재 구현은 아직 `핵심 서비스 문서`의 전체 범위가 아니라, 그중 `PMS / EMS / GMS` 추천 흐름의 최소 검증 버전임

## 9. 참고 메모

- `docs/streaming-platforms-api/*`는 외부 음악 플랫폼 연동 참고 자료다
- 이 문서들은 제품 방향 문서가 아니라 통합 참고 문서이므로, 핵심 구조 결정은 이 가이드와 ADR 문서를 우선한다

## 10. 세션용 한 줄 지시문

새 세션은 추측으로 구조를 바꾸지 말고, 먼저 `docs/PROJECT_GUIDE.md`와 `AGENTS.md`를 읽은 뒤 현재 결정 사항에 맞춰 작업을 시작한다.
