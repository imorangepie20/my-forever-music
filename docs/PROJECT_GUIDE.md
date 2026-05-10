# Project Guide

작성일: `2026-04-29`

이 문서는 `my-forever-music`의 가장 중요한 진입 문서입니다.  
새로운 세션, 새로운 작업자, 새로운 AI 에이전트는 이 문서를 먼저 읽고 현재 프로젝트 방향을 파악해야 합니다.

## 1. 프로젝트 한 줄 요약

`my-forever-music`은 기존 MusicSpace 개념을 바탕으로 다시 만드는 프로젝트이며, 먼저 웹앱을 완성한 뒤 같은 도메인과 UI 자산을 재사용해 Windows 데스크탑 앱까지 확장합니다.

핵심 서비스 원문 정의는 [PROJECT_KEY_SERVICE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_KEY_SERVICE.md) 를 기준으로 봅니다.

사용자가 이 서비스를 왜 반복적으로 쓰는지에 대한 제품 관점은 [product/USER_MUSIC_HOME_VISION.md](/Users/woosungjo/music-space/my-forever-music/docs/product/USER_MUSIC_HOME_VISION.md) 를 함께 봅니다.

PMS 중심 감상 경험과 EMS 공개 playlist 풀 수집/노출 기준은 [product/MUSIC_DISCOVERY_AND_LISTENING_UX.md](/Users/woosungjo/music-space/my-forever-music/docs/product/MUSIC_DISCOVERY_AND_LISTENING_UX.md) 를 따릅니다.

사용자 플로우에는 mock data나 sandbox provider를 기본값으로 노출하지 않습니다. 실제 구현 기준은 [architecture/REAL_IMPLEMENTATION_POLICY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/REAL_IMPLEMENTATION_POLICY.md) 를 따릅니다.

오류는 모든 프로젝트 영역에서 우회, 회피, 임시 처리, 에러 숨김으로 넘기지 않습니다. 실패한 경계와 근본 원인을 확인하고 수정하는 기준 역시 [architecture/REAL_IMPLEMENTATION_POLICY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/REAL_IMPLEMENTATION_POLICY.md) 를 따릅니다.

현재 실행 전략은 [architecture/MACBOOK_LOCAL_FIRST_PLAN.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/MACBOOK_LOCAL_FIRST_PLAN.md) 을 따른다. 즉, 먼저 MacBook 로컬에서 실서비스 기능을 구현하고 시험한 뒤 Ubuntu 서버로 이전한다.

## 1-1. 서비스 핵심 정의

이 프로젝트가 만들려는 실제 서비스는 아래와 같습니다.

- 사용자가 구독 중인 스트리밍 플랫폼을 선택하고 계정을 연결한다
- 해당 플랫폼의 플레이리스트를 가져와 `PMS`에 저장한다
- `PMS`는 특정 플랫폼에 묶이지 않는 사용자 소유 음악 취향 라이브러리다
- 플랫폼을 바꾸더라도 사용자의 playlist, track, 평가, 취향 모델은 계속 유지된다
- 각 트랙의 오디오 특성은 `provider-neutral` 전략으로 보강한다
- 오디오 특성을 직접 확보하지 못한 트랙은 가짜 특성으로 채우지 않고 `unresolved` 상태로 남기며 후속 보강/재시도 정책으로 처리한다
- 사용자 플레이리스트와 행동 데이터를 바탕으로 개인별 취향 모델을 점진적으로 학습한다
- `EMS`는 외부 플랫폼의 공개 플레이리스트와 트렌딩 트랙을 수집하는 외부 탐색 공간이다
- `GMS`는 사용자 모델이 통과시킨 추천 결과가 모이는 개인화 게이트웨이 공간이다
- 사용자가 `GMS` 결과를 평가하면 다시 `PMS` 학습 데이터로 환류된다
- 사용자는 추천 결과를 저장하고 자기 playlist를 만들며 사이트 안에서 음악을 감상한다
- 어느 페이지에서든 음악 재생이 가능하고 페이지 이동 사이에도 플레이어 상태가 유지된다

## 2. 현재 확정된 큰 방향

- 프론트엔드는 `React + TypeScript + Vite`
- 메인 API는 `Spring Boot 3.5.x + Java 21 + Gradle`
- AI/추천 서비스는 `FastAPI`
- 데이터베이스는 `PostgreSQL`
- 마이그레이션은 `Flyway`
- Windows 데스크탑 앱은 후속 단계에서 `Tauri 2`
- API 계약 문서 기준은 `OpenAPI`
- 현재 1차 구현과 시험 서비스 환경은 `MacBook 로컬`
- Ubuntu 서버는 기능 안정화 후 이전하는 2차 단계

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
4. [USER_MUSIC_HOME_VISION.md](/Users/woosungjo/music-space/my-forever-music/docs/product/USER_MUSIC_HOME_VISION.md)
5. [MUSIC_DISCOVERY_AND_LISTENING_UX.md](/Users/woosungjo/music-space/my-forever-music/docs/product/MUSIC_DISCOVERY_AND_LISTENING_UX.md)
6. [REAL_IMPLEMENTATION_POLICY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/REAL_IMPLEMENTATION_POLICY.md)
7. [MACBOOK_LOCAL_FIRST_PLAN.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/MACBOOK_LOCAL_FIRST_PLAN.md)
8. [DATABASE_SETUP_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/DATABASE_SETUP_GUIDE.md)
9. [TECH_STACK.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/TECH_STACK.md)
10. [AUDIO_FEATURE_PROVIDER_STRATEGY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/AUDIO_FEATURE_PROVIDER_STRATEGY.md)
11. [DESKTOP_APP_STRATEGY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/DESKTOP_APP_STRATEGY.md)
12. [SPOTIFY_OAUTH_SETUP.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/SPOTIFY_OAUTH_SETUP.md)
13. [HTTPS_DOMAIN_DEV_SETUP.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/HTTPS_DOMAIN_DEV_SETUP.md)
14. [ADR-001-backend-stack.md](/Users/woosungjo/music-space/my-forever-music/docs/decisions/ADR-001-backend-stack.md)
15. [services/api/README.md](/Users/woosungjo/music-space/my-forever-music/services/api/README.md)
16. [docs/api/README.md](/Users/woosungjo/music-space/my-forever-music/docs/api/README.md)
17. [services/ai/README.md](/Users/woosungjo/music-space/my-forever-music/services/ai/README.md)
18. [UBUNTU_SERVER_RUNBOOK.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/UBUNTU_SERVER_RUNBOOK.md)
19. [UBUNTU_SERVER_SETUP_GUIDE.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/UBUNTU_SERVER_SETUP_GUIDE.md)

## 7. 앞으로 문서를 갱신하는 규칙

- 기술 스택이 바뀌면 `TECH_STACK.md`를 먼저 수정
- 구조 결정이 바뀌면 `ADR`를 추가하거나 수정
- 새 세션이 꼭 알아야 할 공통 방향이 바뀌면 이 `PROJECT_GUIDE.md`를 갱신
- 루트 구조가 바뀌면 `README.md`도 함께 수정
- 오류 처리 기준이 바뀌면 `REAL_IMPLEMENTATION_POLICY.md`와 회귀 하네스를 함께 수정

## 8. 지금 시점의 우선순위

### 1차 집중: Spotify & TIDAL PMS 안정화

1. Spotify OAuth, playlist import, PMS user library 영속 저장 안정화
2. TIDAL 실제 provider와 PMS import 검증
3. ReccoBeats 등 외부 provider 기반 오디오 특성 보강 파이프라인 정리
4. 사용자별 음악 학습 모델 개발
5. 추천 결과 평가 저장과 사용자 제작 playlist 구현
6. 사이트 내부 재생 이벤트, 저장, 스킵, playlist 추가 같은 행동 데이터 모델 정의

### 보류: YouTube Music & Apple Music

- **YouTube Music**: 공식 API가 없어 PMS import 불가. EMS 신호원으로만 검토.
- **Apple Music**: Apple Developer Program membership 필요. 준비 시까지 보류.

### 플랫폼 확장 순서 (확정)

1. **Spotify** ✅ 완료 - 1차 기준 플랫폼
2. **TIDAL** 🟡 검증중 - Spotify 다음 우선순위
3. ~~YouTube Music~~ - 공식 API 없음, EMS 신호원으로만 검토
4. ~~Apple Music~~ - Developer Program 준비 시까지 보류

현재 참고 상태:

- `services/api`는 `Java 21 + Gradle wrapper` 기준으로 로컬 부팅 검증 완료
- `services/api`의 `local` 프로필은 현재 `PostgreSQL` 없이 부팅 가능
- `GET /api/v1/platforms/catalog` 응답 경로 추가 완료
- `POST /api/v1/auth/register` 회원가입 경로 추가 완료
- `POST /api/v1/auth/login` 로그인과 온보딩 복원 경로 추가 완료
- `GET/POST /api/v1/platforms/connections/*` 온보딩 연결 경로 추가 완료
- `POST /api/v1/platforms/oauth/*`는 사용자 플로우에서 실제 Spotify OAuth 설정이 있어야 시작됨
- `GET/POST /api/v1/pms/import/*` PMS playlist import 경로 추가 완료
- `services/api`에는 platform credential 저장소와 playlist provider 추상화가 추가되어 실제 Spotify import를 처리함
- `services/api`는 실제 Spotify playlist listing/item import를 처리하며, 오디오 특성은 현재 legacy Spotify lookup path와 placeholder 저장 구조가 함께 남아 있음
- `services/api`는 Spotify access token 만료 시 refresh token 기반 자동 갱신을 수행함
- `services/api`와 `apps/web`는 refresh 실패 시 `reconnect_required` 상태와 재연결 UX까지 반영함
- `services/api`는 DB 활성 프로필에서 PMS import 결과를 `pms_imported_*` 테이블에 영속 저장함
- `services/api`는 PMS import 직후 정식 `PMS user library` sync를 수행하고, DB 활성 프로필에서는 `pms_user_*` 테이블에 영속 저장함
- `GET /api/v1/pms/workspace/bootstrap`는 현재 정식 `PMS user library -> raw import snapshot -> user-owned database catalog -> empty library` 순서로 소스를 선택함
- 사용자별 음악 학습 모델은 플랫폼 연동과 PMS user library 저장 안정화 이후 개발하는 단계로 고정함
- 플랫폼 카탈로그에는 `TIDAL`, `YouTube Music`, `Apple Music`, `Last.fm`이 포함되며, 확장 순서는 `Spotify -> TIDAL -> YouTube Music`, Apple Music은 개발자 계정 준비 전까지 보류로 고정됨
- 현재 사용자 온보딩의 PMS import는 `Spotify`가 1차 안정화 대상이고, `TIDAL`은 실제 provider가 있으나 검증 단계로 남아 있음
- `GET /api/v1/platforms/lastfm/preview` 경로가 추가되어 공개 Last.fm 사용자명 기준 signal preview를 확인할 수 있음
- `POST /api/v1/platforms/lastfm/profile` 경로가 추가되어 Last.fm 사용자명을 계정에 저장할 수 있음
- `GET/POST /api/v1/platforms/lastfm/scrobbles/*` 경로가 추가되어 최근 scrobble snapshot을 저장하고 다시 `/platforms`에서 확인할 수 있음
- `apps/web`의 `/platforms`는 Last.fm preview 결과를 읽고 top artist를 EMS seed로 복사하거나 계정에 저장할 수 있음
- `apps/web`의 `/platforms`는 저장된 Last.fm profile 기준으로 recent scrobble sync와 snapshot 요약도 제공함
- `POST /api/v1/ems/workspace/analysis`는 저장된 Last.fm scrobble snapshot의 artist recurrence를 우선 blend 하고, 비어 있으면 live top artist 조회로 fallback 함
- `POST /api/v1/gms/recommendations/preview`도 저장된 Last.fm scrobble snapshot의 artist recurrence를 우선 seed artist에 blend 하고, 비어 있으면 live top artist 조회로 fallback 함
- `GET /api/v1/pms/workspace/bootstrap` 응답 검증 완료
- `POST /api/v1/ems/workspace/analysis` 응답 경로 추가 완료
- `POST /api/v1/ems/workspace/overview`는 deterministic EMS 상태와 AI 해석을 묶어 EMS overview 화면에 제공함
- `GET /api/v1/ems/collection/playlists`와 `GET /api/v1/ems/collection/playlists/{playlistId}`는 EMS DB에 저장된 공개 playlist와 ordered track detail을 표시함
- `POST /api/v1/ems/collection/search`는 provider 검색 preview만 수행하고, 별도 저장/가져오기 동작 전까지 EMS 테이블에 결과를 넣지 않음
- `POST /api/v1/platforms/playback/tidal/resolve-track`는 TIDAL 재생 모드에서 타 플랫폼 track metadata를 TIDAL playable target으로 resolve 함
- `POST /api/v1/gms/recommendations/preview`는 `services/ai`와의 브리지까지 검증 완료
- `services/api`는 import 전 PMS workspace가 임의 demo playlist/seed를 노출하지 않고 빈 라이브러리 상태를 반환함
- `pms_track`는 아직 `spotify_*` 이름의 legacy 오디오 특성 저장 구조를 사용하지만, 문서 기준은 provider-neutral 전략으로 전환됨
- PMS import 시 오디오 특성 저장 기준은 [AUDIO_FEATURE_PROVIDER_STRATEGY.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/AUDIO_FEATURE_PROVIDER_STRATEGY.md) 와 [PMS_TRACK_AUDIO_FEATURE_STORAGE.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_TRACK_AUDIO_FEATURE_STORAGE.md) 를 함께 따른다
- `docs/api/README.md`가 API 계약 문서의 공식 진입점으로 추가됨
- DB 활성 프로필에서는 PMS bootstrap이 실제 `pms_*` 테이블 기반으로 응답 가능
- `apps/web`에는 `/platforms` route가 추가되어 preferred PMS source platform을 선택할 수 있음
- `apps/web`에는 `/signup` route가 추가되어 회원가입과 기본 플랫폼 선택이 가능함
- `apps/web`에는 `/login` route가 추가되어 기존 계정 재로그인과 현재 온보딩 단계 복원이 가능함
- `apps/web`는 가입 후 세션을 로컬에 저장하고 `/platforms`에서 Spotify OAuth redirect를 처리할 수 있음
- `apps/web`는 `/pms`에서 platform playlist import와 사용자별 workspace bootstrap을 사용할 수 있음
- `apps/web`는 `/pms`, `/ems`, `/gms-preview`에서 playlist cover, album image, playable track card, global playback dock을 공유함
- `apps/web`의 EMS 화면은 overview와 DB 기반 공개 playlist pool을 탭 없이 한 화면에 표시함
- `apps/web`는 EMS/PMS playlist 재생 시 DB detail track을 읽어 queue로 넘기고, TIDAL 모드에서는 track별 TIDAL target resolve 후 재생함
- `apps/web` 공통 player는 새 재생 시작 전 기존 player state를 초기화하고, provider resolve/stream 준비 중 spinner와 상태 메시지를 표시함
- `GET /api/v1/pms/workspace/bootstrap`는 optional `playlist_id` 기준으로 현재 음악 컨텍스트를 다시 투영함
- `POST /api/v1/gms/recommendations/preview`는 가능하면 synthetic item 대신 `PMS user library`의 실제 playable track으로 재매핑함
- `POST /api/v1/gms/recommendations/feedback`는 GMS 추천 후보에 대한 like/dislike/save/skip 평가를 저장함
- `GET/POST /api/v1/pms/personal-playlists/*`는 사용자 제작 PMS playlist 생성과 GMS 추천 후보 저장을 제공함
- 현재 구현은 아직 `핵심 서비스 문서`의 전체 범위가 아니라, 그중 `PMS / EMS / GMS` 추천 흐름의 최소 검증 버전임

## 9. 참고 메모

- `docs/streaming-platforms-api/*`는 외부 음악 플랫폼 연동 참고 자료다
- 이 문서들은 제품 방향 문서가 아니라 통합 참고 문서이므로, 핵심 구조 결정은 이 가이드와 ADR 문서를 우선한다

## 10. 세션용 한 줄 지시문

새 세션은 추측으로 구조를 바꾸지 말고, 먼저 `docs/PROJECT_GUIDE.md`와 `AGENTS.md`를 읽은 뒤 현재 결정 사항에 맞춰 작업을 시작한다.
