# apps/desktop

Windows 데스크탑 앱용 예약 폴더입니다.

## 목표

- 웹앱 완성 후 `Tauri 2` 기반 Windows 데스크탑 애플리케이션으로 확장
- `apps/web`의 UI 자산과 `packages/*`의 공통 코드 최대 재사용
- 로그인, PMS, EMS, GMS 핵심 흐름을 웹과 같은 도메인 모델로 유지

## 권장 방향

- UI: `React + TypeScript + Vite`
- 데스크탑 셸: `Tauri`
- 백엔드 연결: `services/api`(Spring Boot), `services/ai`(FastAPI) 재사용
- 공통화 대상: 타입, API 클라이언트, 검증 스키마, 디자인 토큰

## 예상 구조

```text
apps/desktop/
├── src/                      # 데스크탑 전용 프론트 코드
├── src-tauri/                # Tauri(Rust) 설정과 네이티브 브리지
└── assets/                   # 아이콘, 번들 리소스
```

## 구현 시점에 먼저 할 일

1. 웹 전용 라우트와 공통 라우트 분리
2. API 클라이언트를 패키지로 승격
3. 로컬 파일 접근, 자동 시작, 업데이트 같은 데스크탑 기능만 별도 추가
