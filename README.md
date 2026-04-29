# my-forever-music

MusicSpace 재구축용 새 프로젝트 루트입니다.

가장 먼저 봐야 하는 통합 가이드는 [docs/PROJECT_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_GUIDE.md) 입니다.

## 디렉토리 구조

```text
my-forever-music/
├── apps/
│   ├── web/                  # 사용자용 프론트엔드
│   └── desktop/              # Windows 데스크탑 앱(Tauri 예정)
├── services/
│   ├── api/                  # Spring Boot 메인 API 백엔드
│   └── ai/                   # AI/추천 서비스
├── packages/
│   ├── shared-types/         # 공통 타입
│   ├── shared-utils/         # 공통 유틸
│   └── design-tokens/        # 디자인 토큰
├── infra/
│   ├── docker/               # 컨테이너 관련 파일
│   ├── nginx/                # 리버스 프록시 설정
│   ├── db/                   # 마이그레이션/시드
│   └── scripts/              # 운영/개발 스크립트
├── docs/
│   ├── architecture/         # 시스템 설계 문서
│   ├── product/              # 제품/기능 문서
│   ├── api/                  # API 계약 문서
│   ├── diagrams/             # draw.io / mermaid 다이어그램
│   └── decisions/            # ADR, 의사결정 기록
├── data/
│   ├── samples/              # 샘플 데이터
│   ├── imports/              # 초기 적재용 데이터
│   └── backups/              # 로컬 백업
└── .github/
    └── workflows/            # CI/CD 워크플로
```

## 설계 의도

- `apps/web`: PMS / EMS / GMS 중심 사용자 UI
- `apps/desktop`: 웹 UI와 도메인 로직을 최대한 재사용하는 Windows 앱 셸
- `services/api`: Spring Boot 기반 인증, 사용자설정, 플레이리스트, 트랙, 플랫폼 연동, 추천 오케스트레이션
- `services/ai`: M1 / M2 / M3 / LLM / 오디오 보강 같은 추천 계열 기능
- `packages/*`: 여러 서비스에서 재사용할 코드
- `infra/*`: 배포와 운영 구성
- `docs/*`: 재구축 문서와 다이어그램

## 선정 스택

- 프론트엔드: `React + TypeScript + Vite`
- 데스크탑: `Tauri 2`
- 메인 API: `Spring Boot 3.5.x + Java 21 + Gradle`
- AI 서비스: `FastAPI + Python 3.12`
- 데이터베이스: `PostgreSQL`
- 캐시/큐 보조: `Redis`
- API 계약: `OpenAPI`
- 마이그레이션: `Flyway`

## 현재 반영 상태

- `apps/web`는 `/Users/woosungjo/music-space/imapplepieTemplate001` 루트 Vite 템플릿 기준으로 복사 완료
- `apps/desktop`는 향후 Windows 앱 개발을 위한 예약 구조 생성 완료
- `services/api`는 Spring Boot 스캐폴드와 `services/ai` preview 호출용 GMS 브리지 엔드포인트 생성 완료
- `services/ai`는 최소 FastAPI 스캐폴드와 추천 preview API 초안 생성 완료
- `infra/nginx`는 로컬/운영용 리버스 프록시 설정 템플릿 생성 완료
- Ubuntu 서버 기준 런북과 Docker/Nginx 템플릿 생성 완료
- 불필요한 서브프로젝트, 빌드 결과물, 의존성 폴더는 제외
- 새 프로젝트 전용 폴더 구조는 그대로 유지
- 웹 우선 개발 후 데스크탑 확장을 전제로 문서화 시작

## 다음 추천 작업

1. `services/api` 로컬 실행 환경(Java/Gradle/PostgreSQL) 연결
2. `apps/desktop`를 고려한 공통 패키지 경계 정리
3. JS 계열은 `pnpm`, 백엔드는 `Gradle`로 운영 기준 확정
4. DB 스키마와 Flyway 마이그레이션 초안 작성
5. 인증/사용자설정/PMS API부터 구현 시작
