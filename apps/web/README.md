# apps/web

`imapplepieTemplate001` 템플릿을 기반으로 가져온 뒤, `my-forever-music`에 맞게 최소 제품 셸로 재정리한 프론트엔드 앱입니다.

제품 관점에서 이 웹앱은 단순 추천 테스트 화면이 아니라, 사용자가 구독 플랫폼의 playlist를 가져오고, 자기 취향을 보존하고, 추천을 평가하고, 사이트 안에서 음악을 감상하는 `개인 음악 홈`입니다.

## 현재 포함된 것

- 최소 라우트 셸
- 음악 서비스용 메인 레이아웃
- `GMS recommendation preview` 테스트 화면
- Spring Boot API 호출 레이어
- 기존 템플릿에서 가져온 공통 HUD 스타일과 카드 컴포넌트

## 핵심 라우트

- `/login`
  - 기존 계정 재로그인
  - 현재 온보딩 다음 단계 복원
  - 로컬 시험 서비스 중 세션 재구성
- `/signup`
  - 계정 생성
  - 기본 스트리밍 플랫폼 선택
  - 가입 성공 후 `/platforms` 다음 단계 안내
- `/`
  - 프로젝트 홈
  - API/AI 연결 상태와 현재 workspace 요약 표시
- `/platforms`
  - 스트리밍 플랫폼 카탈로그 로드
  - 가입 사용자 세션 기준 연결 bootstrap 로드
  - 실제 Spotify OAuth start
  - connect / disconnect 상태 반영
  - Last.fm public username preview 로드
  - Last.fm signal profile 저장
  - Last.fm recent scrobble sync 실행
  - 저장된 scrobble snapshot 확인
  - PMS import 기준 플랫폼 선택
  - Spotify 오디오 특성 기준과 실제 provider 활성 상태 표시
- `/platforms/oauth/callback`
  - Spotify PKCE external callback 처리
  - 연결 성공 결과 확인
  - 다음 단계(`/platforms` 또는 `/pms`)로 이동
- `/pms`
  - 현재 사용자 기준 PMS import bootstrap 로드
  - 연결된 preferred platform의 playlist import 후보를 cover image와 함께 표시
  - 선택한 playlist를 PMS로 import
  - 선택된 playlist hero, playlist shelf, track shelf, global player 표시
  - seed track / artist / genre / playlist 입력
  - bootstrap track별 album image, playback target, Spotify 오디오 특성 readiness 표시
- `/ems`
  - 연결된 provider 전체 검색
  - playlist/track 검색 결과 페이지 유지
  - 검색 playlist 클릭 후 track detail 페이지에서 전체/개별 재생
  - EMS DB public playlist pool 표시와 저장된 playlist 재생
- `/gms-preview`
  - `POST /api/v1/gms/recommendations/preview` 호출
  - PMS/EMS workspace 값을 바탕으로 요청 생성
  - context, warnings, preview 결과 확인
  - PMS user library에서 재매핑된 playable track card와 global player 표시

## 공통 플레이어

- 모든 주요 음악 페이지는 하단 고정 `global playback dock`를 공유한다.
- 현재는 `Spotify embed` 또는 `preview_url`이 있는 경우 inline playback이 가능하다.
- inline playback이 없으면 외부 플랫폼 링크로 fallback 한다.

## API 연결 방식

- 기본값은 same-origin 호출입니다.
- 로컬 Vite 개발에서는 `vite.config.ts`의 proxy가 `/api`, `/actuator`를 `http://localhost:8081`으로 전달합니다.
- 필요하면 `VITE_API_BASE_URL`로 별도 API 주소를 줄 수 있습니다.
- 문서 링크 주소를 따로 주고 싶으면 `VITE_API_DOCS_URL`, `VITE_AI_DOCS_URL`도 사용할 수 있습니다.
- HTTPS reverse proxy 뒤에서 도메인 개발을 할 때는 `.env.local`의 `VITE_PUBLIC_HOST`, `VITE_HMR_PROTOCOL`, `VITE_HMR_CLIENT_PORT`로 HMR websocket 기준을 맞춥니다.

## 도메인 HTTPS 개발

- 예시 env 파일: [apps/web/.env.example](/Users/woosungjo/music-space/my-forever-music/apps/web/.env.example)
- 현재 로컬 적용값은 `apps/web/.env.local`에 들어 있습니다.
- 실행 스크립트:
  [run-web-with-domain-env.sh](/Users/woosungjo/music-space/my-forever-music/infra/scripts/run-web-with-domain-env.sh)
- 전체 절차:
  [HTTPS_DOMAIN_DEV_SETUP.md](/Users/woosungjo/music-space/my-forever-music/docs/architecture/HTTPS_DOMAIN_DEV_SETUP.md)

## 다음 추천 작업

1. 추천 결과 평가와 저장 UI 추가
2. 사용자 제작 playlist 생성, 이름 변경, track 추가 UI 추가
3. 사이트 내부 playback event와 skip/save/add-to-playlist 행동 수집 연결
4. 저장된 Last.fm signal profile과 scrobble snapshot을 EMS/GMS UI 설명에 더 직접 반영
5. 데스크탑 앱 재사용을 고려해 API 클라이언트 모듈 분리
