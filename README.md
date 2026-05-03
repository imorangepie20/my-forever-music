# my-forever-music

MusicSpace 재구축용 새 프로젝트 루트입니다.

가장 먼저 봐야 하는 통합 가이드는 [docs/PROJECT_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_GUIDE.md) 입니다.

핵심 서비스 정의 원문은 [docs/PROJECT_KEY_SERVICE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_KEY_SERVICE.md) 에 정리되어 있습니다.

## 서비스 한 줄 정의

`my-forever-music`은 사용자가 구독 중인 음악 스트리밍 플랫폼의 플레이리스트를 가져와 음악 취향을 분석하고, 외부 트렌드 플레이리스트와 결합해 개인화된 추천 흐름을 만드는 서비스입니다.

핵심 도메인은 아래 3개 공간으로 나뉩니다.

- `PMS`: 사용자의 플레이리스트와 선호 데이터를 쌓는 개인 음악 공간
- `EMS`: 외부 플랫폼의 공개 플레이리스트와 트렌드를 수집하는 탐색 공간
- `GMS`: 사용자 모델이 평가해 통과시킨 추천 결과가 모이는 게이트웨이 공간

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
- `services/ai`: 오디오 특성 보강, 사용자 취향 모델, EMS/GMS 추천 계산 같은 AI 계열 기능
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

- 제품 목표는 `스트리밍 플랫폼 플레이리스트 수집 -> 오디오 특성 분석 -> 사용자 취향 모델 -> EMS/GMS 추천 루프` 구조로 정의됨
- `apps/web`는 Vite 기반 최소 제품 셸과 `GMS preview` 테스트 화면까지 정리 완료
- `apps/web`는 `/signup` 화면에서 회원가입과 기본 스트리밍 플랫폼 선택 가능
- `apps/web`는 `/platforms` 화면에서 스트리밍 플랫폼 카탈로그, 가입 사용자 세션, sandbox 연결/해제와 Spotify OAuth redirect 흐름을 제공
- `apps/web`는 `/pms` 화면에서 사용자별 PMS bootstrap과 platform playlist import를 제공
- `apps/web`는 `PMS import/bootstrap -> EMS workspace -> GMS preview` 흐름까지 반영 완료
- `apps/web`의 `EMS` 화면은 Spring Boot `workspace analysis` 결과를 받아 추천값 적용 가능
- `apps/desktop`는 향후 Windows 앱 개발을 위한 예약 구조 생성 완료
- `services/api`는 `GET /api/v1/platforms/catalog` 엔드포인트로 플랫폼 역할과 온보딩 흐름 제공
- `services/api`는 `POST /api/v1/auth/register` 엔드포인트로 회원가입과 기본 플랫폼 선택 제공
- `services/api`는 `GET/POST /api/v1/platforms/connections/*` 엔드포인트로 가입 직후 플랫폼 연결 온보딩 제공
- `services/api`는 `GET/POST /api/v1/pms/import/*` 엔드포인트로 PMS playlist import 제공
- `services/api`는 platform credential 저장과 playlist provider 추상화를 통해 sandbox와 실제 Spotify import를 같은 흐름에서 처리함
- `services/api`는 Spotify PKCE draft 시작 URL, external callback, authorization code token exchange까지 반영됨
- `services/api`는 실제 Spotify playlist 목록과 playlist item import 경로를 추가함
- `services/api`는 PMS workspace bootstrap과 `services/ai` preview 호출용 GMS 브리지 엔드포인트 생성 완료
- `services/api`는 PMS seed 기반 `EMS workspace analysis` 엔드포인트 추가 완료
- `services/api`는 `local` 프로필 기준으로 DB 없이 로컬 부팅 검증 완료
- `services/api`의 `PMS bootstrap`과 `GMS preview -> services/ai` 브리지 응답 검증 완료
- `services/api`는 PMS bootstrap용 `Flyway + JPA` 최소 카탈로그와 demo seed 데이터 추가 완료
- `services/api`의 `pms_track`는 Spotify 오디오 특성 전체 스냅샷을 저장할 수 있게 확장됨
- PMS import 시 Spotify 오디오 특성 전체 저장 기준 문서가 추가됨
- `services/api`는 DB 활성 프로필에서 `pms_playlist / pms_track / pms_playlist_track` 기반 bootstrap 응답 가능
- `services/ai`는 최소 FastAPI 스캐폴드와 추천 preview API 초안 생성 완료
- `infra/nginx`는 로컬/운영용 리버스 프록시 설정 템플릿 생성 완료
- Ubuntu 서버 기준 런북과 Docker/Nginx 템플릿 생성 완료
- 불필요한 서브프로젝트, 빌드 결과물, 의존성 폴더는 제외
- 새 프로젝트 전용 폴더 구조는 그대로 유지
- 웹 우선 개발 후 데스크탑 확장을 전제로 문서화 시작

아직 미구현이거나 sandbox 단계인 핵심 서비스 문서의 목표:

- Spotify access token refresh와 장기 세션 유지
- Apple Music / TIDAL 실제 플랫폼 연동
- PMS import 결과의 영속 사용자/플레이리스트 동기화
- 사용자 행동 데이터 기반 개인화 모델 업데이트
- EMS 외부 플레이리스트 수집, GMS 추천 통과, 사용자 평가의 PMS 환류
- 페이지 이동 간 유지되는 공통 음악 플레이어

## 다음 추천 작업

1. Spotify access token refresh와 만료 복구 흐름 추가
2. PMS import 결과를 영속 사용자/플레이리스트 데이터 모델로 확장
3. Apple Music / TIDAL 실제 플랫폼 provider 설계
4. EMS 외부 플레이리스트 수집과 GMS 환류 구조 설계
5. 공통 플레이어와 데스크탑 재사용을 고려한 프론트 구조 정리

## 로컬 DB 참고

- [infra/docker/README.md](/Users/woosungjo/music-space/my-forever-music/infra/docker/README.md)
- [infra/docker/docker-compose.local-db.yml](/Users/woosungjo/music-space/my-forever-music/infra/docker/docker-compose.local-db.yml)

## 구현 기준 문서

- [docs/PROJECT_KEY_SERVICE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_KEY_SERVICE.md)
- [docs/architecture/SPOTIFY_OAUTH_SETUP.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/SPOTIFY_OAUTH_SETUP.md)
- [docs/api/README.md](/Users/woosungjo/music-space/my-forever-music/docs/api/README.md)
- [docs/api/AUTH_REGISTER_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/AUTH_REGISTER_API.md)
- [docs/api/PLATFORM_CONNECTION_ONBOARDING_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PLATFORM_CONNECTION_ONBOARDING_API.md)
- [docs/api/PMS_PLAYLIST_IMPORT_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_PLAYLIST_IMPORT_API.md)
- [docs/api/PMS_TRACK_AUDIO_FEATURE_STORAGE.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_TRACK_AUDIO_FEATURE_STORAGE.md)
