# my-forever-music

MusicSpace 재구축용 새 프로젝트 루트입니다.

가장 먼저 봐야 하는 통합 가이드는 [docs/PROJECT_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_GUIDE.md) 입니다.

핵심 서비스 정의 원문은 [docs/PROJECT_KEY_SERVICE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_KEY_SERVICE.md) 에 정리되어 있습니다.

회원이 이 사이트를 반복적으로 이용하는 이유와 장기 제품 방향은 [docs/product/USER_MUSIC_HOME_VISION.md](/Users/woosungjo/music-space/my-forever-music/docs/product/USER_MUSIC_HOME_VISION.md) 에 정리되어 있습니다.

현재 작업 전략은 [docs/architecture/MACBOOK_LOCAL_FIRST_PLAN.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/MACBOOK_LOCAL_FIRST_PLAN.md) 을 기준으로 합니다. 우선 이 MacBook 로컬 시스템에서 실서비스 기능을 구현하고 시험한 뒤 Ubuntu 서버로 이전합니다.

## 서비스 한 줄 정의

`my-forever-music`은 사용자가 구독 중인 음악 스트리밍 플랫폼의 플레이리스트를 가져와 자기 소유의 음악 취향 라이브러리로 보존하고, 플랫폼을 옮겨도 유지되는 개인화 추천과 음악 감상 경험을 제공하는 서비스입니다.

핵심 도메인은 아래 3개 공간으로 나뉩니다.

- `PMS`: 사용자의 플레이리스트, 선호, 평가, 행동 데이터를 보존하는 개인 음악 공간
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
- 제품 중심 가치는 `스트리밍 플랫폼이 바뀌어도 유지되는 사용자 소유 playlist/taste library`로 정의됨
- `apps/web`는 Vite 기반 최소 제품 셸과 `GMS preview` 테스트 화면까지 정리 완료
- `apps/web`는 `/signup` 화면에서 회원가입과 기본 스트리밍 플랫폼 선택 가능
- `apps/web`는 `/login` 화면에서 기존 계정 재로그인과 온보딩 복원 가능
- `apps/web`는 `/platforms` 화면에서 스트리밍 플랫폼 카탈로그, 가입 사용자 세션, 실제 Spotify OAuth redirect 흐름과 Last.fm signal profile 저장을 제공
- `apps/web`는 `/platforms` 화면에서 Last.fm 공개 사용자명 기준 signal preview와 EMS seed artist 반영을 제공
- `apps/web`는 `/platforms` 화면에서 Last.fm username 저장, 최근 scrobble sync, 저장 snapshot 확인까지 제공
- `apps/web`는 `/pms` 화면에서 사용자별 PMS bootstrap과 platform playlist import를 제공
- `apps/web`는 `PMS import/bootstrap -> EMS workspace -> GMS preview` 흐름까지 반영 완료
- `apps/web`의 `EMS` 화면은 provider 검색, 검색 playlist track detail/playback, EMS DB 공개 playlist pool을 한 화면 흐름으로 제공
- `apps/web` 공통 플레이어는 새 재생 시작 전 초기화한 뒤 TIDAL resolve/stream 준비 상태를 spinner와 메시지로 표시
- `apps/desktop`는 향후 Windows 앱 개발을 위한 예약 구조 생성 완료
- `services/api`는 `GET /api/v1/platforms/catalog` 엔드포인트로 플랫폼 역할과 온보딩 흐름 제공
- 플랫폼 카탈로그에는 `TIDAL`, `YouTube Music`, `Apple Music`, `Last.fm`까지 포함되며, 확장 순서는 `Spotify -> TIDAL -> YouTube Music`, Apple Music은 개발자 계정 준비 전까지 보류로 고정됨
- `services/api`는 `GET /api/v1/platforms/lastfm/preview` 엔드포인트로 최근 scrobble, top artist, top track preview를 제공
- `services/api`는 `POST /api/v1/platforms/lastfm/profile` 엔드포인트로 Last.fm signal profile 저장과 EMS 재사용 경로를 제공
- `services/api`는 `GET/POST /api/v1/platforms/lastfm/scrobbles/*` 엔드포인트로 scrobble snapshot 저장과 bootstrap 조회를 제공
- `services/api`는 저장된 Last.fm scrobble snapshot을 `EMS workspace analysis`와 `GMS recommendation preview`에 우선 반영하고, 없으면 live top artist 조회로 fallback 함
- `services/api`와 `apps/web`는 GMS 추천 후보의 like/dislike/save 평가를 저장해 PMS 학습 신호로 환류할 수 있음
- `services/api`와 `apps/web`는 PMS personal playlist 생성과 GMS 추천 후보 저장을 제공함
- `services/api`는 `POST /api/v1/auth/register` 엔드포인트로 회원가입과 기본 플랫폼 선택 제공
- `services/api`는 `POST /api/v1/auth/login` 엔드포인트로 기존 계정 로그인과 현재 온보딩 복원을 제공
- `services/api`는 `GET/POST /api/v1/platforms/connections/*` 엔드포인트로 가입 직후 플랫폼 연결 온보딩 제공
- `services/api`는 `GET/POST /api/v1/pms/import/*` 엔드포인트로 PMS playlist import 제공
- `services/api`는 platform credential 저장과 playlist provider 추상화를 통해 실제 Spotify playlist import를 처리함
- `services/api`는 Spotify PKCE draft 시작 URL, external callback, authorization code token exchange까지 반영됨
- `services/api`는 실제 Spotify playlist 목록과 playlist item import 경로를 추가함
- `services/api`는 Spotify access token 만료 시 refresh token 기반 자동 갱신을 지원함
- `services/api`와 `apps/web`는 refresh 실패 시 `reconnect_required` 상태와 재연결 UX까지 반영함
- `services/api`는 DB 활성 프로필에서 PMS import 결과를 `pms_imported_*` 테이블로 영속 저장함
- `services/api`는 PMS import 직후 정식 `PMS user library` sync를 수행하고, DB 활성 프로필에서는 `pms_user_*` 테이블로도 영속 저장함
- `services/api`의 `PMS workspace bootstrap`는 현재 정식 `PMS user library`를 raw import snapshot보다 우선 사용함
- `services/api`는 PMS workspace bootstrap과 `services/ai` preview 호출용 GMS 브리지 엔드포인트 생성 완료
- `services/api`는 PMS seed 기반 `EMS workspace analysis` 엔드포인트 추가 완료
- `services/api`는 EMS provider search 결과를 `search_pool`에 저장하고, search playlist track detail, EMS collection browse/detail, EMS overview, TIDAL playback target resolve endpoint를 제공
- `services/api`는 `local` 프로필 기준으로 DB 없이 로컬 부팅 검증 완료
- `services/api`의 `PMS bootstrap`과 `GMS preview -> services/ai` 브리지 응답 검증 완료
- `services/api`는 import 전 PMS workspace가 가짜 seed를 노출하지 않도록 빈 라이브러리 상태를 반환함
- `services/api`의 `pms_track`는 Spotify 오디오 특성 전체 스냅샷을 저장할 수 있게 확장됨
- PMS import 시 Spotify 오디오 특성 전체 저장 기준 문서가 추가됨
- `services/api`는 DB 활성 프로필에서 `pms_playlist / pms_track / pms_playlist_track` 기반 bootstrap 응답 가능
- `services/api`는 EMS search 결과 적재의 silent skip을 제거함: `getSearchPlaylistTracks`가 진입 시 `ems_collected_playlist` row를 항상 보장하고, `storeSearchPlaylistTracks`는 playlist 인자를 필수로 받아 null이면 실패시킴
- `services/api`는 EMS search 적재 후 트랙이 0개로 확인된 신규 playlist는 본 테이블에 남기지 않으며, 이미 적재된 빈 playlist는 `POST /api/v1/ems/collection/admin/playlists/cleanup-empty`로 일괄 정리할 수 있음
- `services/api`는 TIDAL `getPlaylistTracks`가 legacy 2xx 빈 결과를 진짜 0개로 신뢰하고, legacy 실패 시에만 OpenAPI fallback을 호출하며 N+1 fallback에서 track detail이 drop된 개수를 WARN으로 로깅함
- `services/api`는 EMS pool worker race를 `FOR UPDATE SKIP LOCKED` 기반 entry claim과 별도 `EmsPoolEntryClaimer` 빈으로 차단함 (self-invocation으로 `@Transactional`이 무시되던 문제 해소)
- `services/api`의 EMS pool entry processor 가드는 `running` 상태 entry도 처리하도록 보완되고, admin "다시 실행" 진입 시 stuck `running` entry를 `queued`로 reset해 복구 가능함
- `services/api`는 EMS pool admin 경로에 entry 단위 재시도(`POST .../entries/{entryId}/retry`), run 삭제(`DELETE .../pool/runs/{runId}`, FK CASCADE), 빈 EMS 플레이리스트 일괄 정리 엔드포인트를 제공함
- `apps/web`의 `/ems/pool-admin`은 polling을 AbortController로 race-safe하게 처리하고 페이지 hidden 시 polling을 중단하며, entry 단위 재시도, run 삭제, 빈 EMS 플레이리스트 일괄 정리, last_error tooltip 표시를 제공함
- `apps/web`의 공통 컴포넌트로 HUD 템플릿 스타일의 재사용 가능한 `ConfirmDialog`가 추가되어 pool-admin의 위험 동작을 모두 동일한 다이얼로그로 확인받음
- `services/api`는 `GET /api/v1/gms/playlists/preview`와 `POST /api/v1/gms/playlists/{id}/save` 엔드포인트로 EMS 평가 플레이리스트를 사용자 PMS 라이브러리에 직접 저장하는 GMS playlist 흐름을 제공함 (cold-start 사용자는 409, 결정적 personal playlist id 사용으로 멱등 추가)
- `services/api`의 GMS playlist preview는 후보마다 6축(affinity/novelty/coherence/diversity/redundancy/confidence) evidence와 composite score를 계산해 응답에 포함하고, composite score 기준으로 후보를 재정렬함
- `apps/web`는 `/gms-playlists` 화면에서 사용자에게 EMS 평가 플레이리스트 후보를 composite/affinity 점수와 6축 evidence 패널과 함께 카드로 노출하고, "Preview tracks" 모달에서 트랙 목록과 개별/전체 재생을 시청한 뒤 ConfirmDialog로 PMS 저장을 승인받음
- `services/ai`는 최소 FastAPI 스캐폴드와 추천 preview API 초안 생성 완료
- `infra/nginx`는 로컬/운영용 리버스 프록시 설정 템플릿 생성 완료
- Ubuntu 서버 기준 런북과 Docker/Nginx 템플릿 생성 완료
- 불필요한 서브프로젝트, 빌드 결과물, 의존성 폴더는 제외
- 새 프로젝트 전용 폴더 구조는 그대로 유지
- 웹 우선 개발 후 데스크탑 확장을 전제로 문서화 시작
- 현재 1차 구현/시험 서비스 환경은 `MacBook 로컬`, Ubuntu는 다음 이전 단계로 정리됨

아직 실제 provider 구현 전이라 사용자 플로우에 열지 않는 핵심 서비스 문서의 목표:

- Spotify 장기 세션 운영 고도화와 refresh 실패 관측/운영 정책
- TIDAL 실제 PMS provider 연동
- YouTube Music 실제 PMS provider 연동
- Apple Music 실제 PMS provider 연동은 개발자 계정 준비 후 진행
- Last.fm scrobble 주기 동기화와 시계열 취향 변화 반영
- 사용자 행동 데이터 기반 개인화 모델 업데이트
- 사용자별 음악 학습 모델 개발
- 정식 `PMS user library` 이후의 사용자 편집/평가 도메인 확장
- 사용자 제작 playlist와 추천 결과 저장/평가 도메인 확장
- 사이트 내부 음악 감상 행동 이벤트 저장
- EMS 외부 플레이리스트 수집, GMS 추천 통과, 사용자 평가의 PMS 환류
- 페이지 이동 간 유지되는 공통 음악 플레이어의 행동 이벤트 저장

## 다음 추천 작업

1. MacBook 로컬에서 핵심 사용자 플로우를 끝까지 안정화
2. Spotify OAuth, playlist import, PMS user library 영속 저장 안정화
3. TIDAL 실제 플랫폼 provider 설계와 PMS import 검증
4. YouTube Music 실제 플랫폼 provider 설계
5. 사용자별 음악 학습 모델 개발
6. 추천 결과 평가 저장과 사용자 제작 playlist 구현

## 로컬 데이터베이스 설정

### 전체 MacBook 스택 재시작

Docker DB/Redis, HTTPS 도메인 프록시, FastAPI AI, Spring Boot API, Vite 웹 서버를 함께 다시 시작하려면:

```bash
./infra/scripts/restart-macbook-stack.sh
```

실행 후 주요 URL:
- TIDAL SDK 테스트 페이지: `https://imapplepie20.tplinkdns.com/tidal-playlist-test`
- API health: `http://127.0.0.1:8081/actuator/health`
- AI health: `http://127.0.0.1:8000/health`
- 로컬 실행 로그: `tmp/local-stack/logs/`

### Docker로 PostgreSQL/Redis 실행

```bash
cd infra/docker
docker compose -f docker-compose.local-db.yml up -d
```

포트 설정:
- PostgreSQL: `127.0.0.1:5433` (다른 서비스와의 충돌 방지)
- Redis: `127.0.0.1:6379`

### Spring Boot database 프로필 실행

```bash
cd services/api
SPRING_PROFILES_ACTIVE=database DB_PORT=5433 ./gradlew bootRun
```

`database` 프로필은:
- PostgreSQL 연결 (port 5433)
- Flyway 마이그레이션 자동 실행
- JPA/Hibernate 영속성 활성화

### 데이터베이스 직접 접속

```bash
docker exec -it my-forever-music-local-postgres psql -U postgres -d my_forever_music
```

### 컨테이너 상태 확인

```bash
cd infra/docker
docker compose -f docker-compose.local-db.yml ps
```

## 로컬 DB 참고

- [infra/docker/README.md](/Users/woosungjo/music-space/my-forever-music/infra/docker/README.md)
- [infra/docker/docker-compose.local-db.yml](/Users/woosungjo/music-space/my-forever-music/infra/docker/docker-compose.local-db.yml)

## 구현 기준 문서

- [docs/PROJECT_KEY_SERVICE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_KEY_SERVICE.md)
- [docs/architecture/MACBOOK_LOCAL_FIRST_PLAN.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/MACBOOK_LOCAL_FIRST_PLAN.md)
- [docs/architecture/SPOTIFY_OAUTH_SETUP.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/SPOTIFY_OAUTH_SETUP.md)
- [docs/api/README.md](/Users/woosungjo/music-space/my-forever-music/docs/api/README.md)
- [docs/api/AUTH_REGISTER_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/AUTH_REGISTER_API.md)
- [docs/api/PLATFORM_CONNECTION_ONBOARDING_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PLATFORM_CONNECTION_ONBOARDING_API.md)
- [docs/api/PMS_PLAYLIST_IMPORT_API.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_PLAYLIST_IMPORT_API.md)
- [docs/api/PMS_TRACK_AUDIO_FEATURE_STORAGE.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_TRACK_AUDIO_FEATURE_STORAGE.md)
