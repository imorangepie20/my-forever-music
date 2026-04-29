# ADR-001 Backend Stack

작성일: `2026-04-29`

## 상태

승인

## 결정

메인 백엔드 서비스는 `Node/NestJS` 대신 `Spring Boot`로 구축한다.

권장 기준은 다음과 같다.

- `Spring Boot 3.5.x`
- `Java 21`
- `Gradle`
- `Spring Web`
- `Spring Security`
- `Spring Data JPA`
- `Actuator`
- `Flyway`

AI/추천 전용 서비스는 별도로 `FastAPI`를 유지한다.

## 배경

- 레거시 분석에서 Spring 계열 구조와 도메인 책임이 더 자연스럽게 이어졌다
- 인증, 트랜잭션, 검증, 운영 모니터링 요구가 강하다
- PMS / EMS / GMS 흐름은 도메인 계층과 데이터 계층의 안정성이 중요하다
- 이후 Windows 데스크탑 앱이 추가되어도 API는 동일하게 재사용해야 한다

## 기대 효과

- 도메인 API 구조 안정화
- 운영 엔드포인트와 헬스체크 표준화
- JPA/Flyway 기반 데이터 관리 일관성 확보
- 웹앱과 데스크탑 앱이 같은 API 계약을 재사용 가능

## 트레이드오프

- 프론트와 백엔드가 단일 언어가 아니게 된다
- JS 계열 워크스페이스와 Java 빌드 체계를 함께 운영해야 한다
- 초기 스캐폴딩과 로컬 실행 스크립트가 조금 더 복합적이다

## 후속 작업

1. `services/api`를 Spring Boot 구조로 고정
2. OpenAPI 문서 기준 확정
3. 인증과 사용자 설정부터 API 설계 시작
4. Docker Compose와 로컬 실행 스크립트 정리
