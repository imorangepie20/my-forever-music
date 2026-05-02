# Tech Stack

작성일: `2026-04-29`

## 최종 방향

이 프로젝트는 `웹앱 우선`으로 개발하고, 이후 같은 도메인과 UI 자산을 재사용해 `Windows 데스크탑 앱`으로 확장합니다.

메인 백엔드는 `Node/NestJS` 대신 `Spring Boot`를 사용합니다.

## 권장 스택

- `apps/web`: `React 19` + `TypeScript` + `Vite`
- `apps/desktop`: `Tauri 2`
- `services/api`: `Spring Boot 3.5.x` + `Java 21` + `Gradle`
- `services/ai`: `FastAPI` + `Python 3.12` + `Pydantic 2`
- `database`: `PostgreSQL`
- `cache`: `Redis`
- `migration`: `Flyway`
- `api contract`: `OpenAPI`
- `frontend test`: `Vitest`, `Playwright`
- `backend test`: `JUnit 5`, `Spring Boot Test`, `Testcontainers`

## 서비스별 역할

- `apps/web`: 브라우저 사용자 UI
- `apps/desktop`: Tauri 기반 Windows 셸
- `services/api`: 인증, 사용자, PMS, EMS, GMS 오케스트레이션
- `services/ai`: 추천 계산, 임베딩, 모델 추론, AI 보강
- `packages/shared-types`: 프론트에서 사용하는 API 타입과 도메인 타입
- `packages/shared-utils`: 포맷터, 클라이언트 헬퍼, 공통 로직
- `packages/design-tokens`: 색상, 간격, 타이포 등 디자인 자산

## 제품 데이터 기준

- 트랙의 핵심 분석 기준은 우선 `Spotify` 오디오 특성을 기준으로 삼는다
- 주요 분석 항목은 `danceability`, `energy`, `valence`, `acousticness`, `liveness`, `speechiness`, `tempo`다
- 플레이리스트 import 시점에는 트랙별 `Spotify 오디오 특성 전체 스냅샷`을 채워 저장하는 것을 기본 원칙으로 삼는다
- 사용자의 재생 횟수, 청취 시간, skip, save, playlist 추가 같은 행동 데이터도 장기적으로 함께 학습한다
- 특정 트랙의 오디오 특성을 직접 확보하지 못하면 웹 검색과 보강 로직으로 fallback 특성을 생성한다
- `services/ai`는 장기적으로 약 `10만 곡` 수준의 기반 트랙 데이터와 사용자별 추가 학습 데이터를 함께 다루는 구조를 목표로 한다

## 도메인 모델 기준

- `PMS`: 사용자의 원본 플레이리스트, seed, 평가, 학습 재료를 저장하는 개인 공간
- `EMS`: 외부 플랫폼의 공개 플레이리스트와 트렌딩 트랙을 수집하는 외부 탐색 공간
- `GMS`: 사용자 모델이 통과시킨 추천 결과와 후보를 보여주는 게이트웨이 공간

## Spring Boot를 선택한 이유

- 레거시 분석상 도메인 중심 API와 Spring 구조가 더 자연스럽게 이어짐
- 인증, 트랜잭션, 검증, 배치, 운영 모니터링이 강함
- `Actuator`로 `/actuator/health` 같은 운영 엔드포인트를 바로 붙이기 쉬움
- `Spring Data JPA`와 `Flyway` 조합으로 데이터 계층을 안정적으로 운영하기 좋음
- 데스크탑 앱이 추가되어도 API 계층은 그대로 재사용 가능함

## `services/api` 기본 구성

- 웹: `Spring Web`
- 검증: `Bean Validation`
- 보안: `Spring Security`
- 데이터: `Spring Data JPA`
- 운영: `Actuator`
- 마이그레이션: `Flyway`
- 문서화: `springdoc-openapi` 계열

## 운영 기준

- JS/TS 계열 패키지 관리: `pnpm`
- Java 빌드: `Gradle`
- Python 의존성 관리는 추후 `uv` 또는 `poetry` 중 하나로 고정
- 컨테이너/로컬 개발은 `Docker Compose`

## 버전 선택 메모

- Spring Boot 공식 시스템 요구사항 문서는 현재 `4.0.6` stable을 기준으로 `Java 17+`를 요구함
- 같은 공식 문서에 stable 라인으로 `3.5.14`, `3.4.13`, `3.3.13`도 함께 표시됨
- 이 프로젝트는 새로 시작하지만 보수적 운영을 위해 `Spring Boot 3.5.x + Java 21`을 우선 권장함

이 판단은 `2026-04-29` 기준의 공식 문서를 참고한 추천입니다.

## 공식 참고

- Spring Boot system requirements: `https://docs.spring.io/spring-boot/system-requirements.html`
- Spring Boot project page: `https://spring.io/projects/spring-boot/`
- Spring Boot actuator endpoints: `https://docs.spring.io/spring-boot/reference/actuator/endpoints.html`
