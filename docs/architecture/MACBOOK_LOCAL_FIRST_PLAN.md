# MacBook Local-First Plan

작성일: `2026-05-04`

## 목적

현재 `my-forever-music`은 먼저 이 MacBook 로컬 시스템에서 실제 서비스 기능을 끝까지 구현하고, 충분히 시험 서비스한 뒤 Ubuntu 서버로 이전하는 전략을 따릅니다.

즉, 지금 단계의 1차 목표는 `Ubuntu 배포 최적화`가 아니라 `MacBook에서 서비스 완성도 확보`입니다.

## 현재 기본 원칙

1. 주 개발 환경은 `MacBook 로컬`
2. 기능 구현, 브라우저 확인, OAuth 확인, PMS/EMS/GMS 흐름 검증은 먼저 로컬에서 끝낸다
3. Ubuntu 문서는 `즉시 운영 기준`이 아니라 `다음 승격 단계용 준비 문서`로 취급한다
4. 서버 이전은 기능 안정화와 시험 서비스 확인 후 진행한다

## 현재 기준 실행 환경

- `apps/web`: MacBook에서 `Vite dev server`
- `services/api`: MacBook에서 `Spring Boot`
- `services/ai`: MacBook에서 `FastAPI`
- `PostgreSQL / Redis`: 필요 시 로컬 Docker 사용
- 외부 테스트: 필요하면 현재 도메인/Nginx 경로를 붙여서 OAuth와 브라우저 플로우를 확인

## 작업 우선순위

### 1. 지금 당장 우선할 일

- 회원가입, 플랫폼 연결, PMS import, EMS 분석, GMS preview를 로컬에서 완성
- Last.fm, Spotify 같은 외부 연동을 로컬 기준으로 안정화
- 문서, API 계약, DB 스키마를 로컬 구현 기준으로 먼저 고정

### 2. 시험 서비스 단계

- 실제 브라우저 사용 흐름으로 주요 시나리오 반복 검증
- OAuth redirect, reconnect, import, analysis, preview 실패 흐름까지 확인
- 로컬 DB를 붙인 상태와 `local` 프로필 상태를 둘 다 확인

### 3. Ubuntu 이전 단계

- 로컬에서 검증된 버전만 서버로 이동
- 환경 변수, Nginx, HTTPS, Docker/DB 운영값을 서버용으로 치환
- systemd 또는 Compose 기준 기동 방식 확정

## Ubuntu로 넘기기 전 체크리스트

- `apps/web` 빌드 성공
- `services/api` 테스트 성공
- `services/ai` 테스트 또는 최소 실행 검증 성공
- 주요 사용자 플로우 수동 검증 완료
- DB 마이그레이션 정리 완료
- `.env.local`과 서버용 env 항목 분리 완료
- Spotify / Last.fm redirect 및 callback 경로 정리 완료
- Nginx 경로와 포트 전략 고정 완료

## 새 세션 작업 원칙

- 새 기능은 먼저 `MacBook 로컬에서 바로 확인 가능한 형태`로 구현한다
- Ubuntu 전용 최적화는 기능 검증을 막지 않는 범위에서만 한다
- 서버 이전 문서를 먼저 늘리기보다 로컬 서비스 완성도를 우선한다
- 배포 관련 변경을 하더라도 로컬 실행 흐름을 깨지 않게 유지한다

## 관련 문서

- [README.md](/Users/woosungjo/music-space/my-forever-music/README.md)
- [PROJECT_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_GUIDE.md)
- [UBUNTU_SERVER_RUNBOOK.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/UBUNTU_SERVER_RUNBOOK.md)
- [UBUNTU_SERVER_SETUP_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/UBUNTU_SERVER_SETUP_GUIDE.md)
