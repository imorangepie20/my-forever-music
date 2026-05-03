# apps/web

`imapplepieTemplate001` 템플릿을 기반으로 가져온 뒤, `my-forever-music`에 맞게 최소 제품 셸로 재정리한 프론트엔드 앱입니다.

## 현재 포함된 것

- 최소 라우트 셸
- 음악 서비스용 메인 레이아웃
- `GMS recommendation preview` 테스트 화면
- Spring Boot API 호출 레이어
- 기존 템플릿에서 가져온 공통 HUD 스타일과 카드 컴포넌트

## 핵심 라우트

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
  - sandbox OAuth start
  - connect / disconnect 상태 반영
  - PMS import 기준 플랫폼 선택
  - Spotify 오디오 특성 기준과 fallback 전략 표시
- `/platforms/oauth/authorize`
  - sandbox provider approval 화면
  - requested scope 확인
  - callback route로 이동
- `/platforms/oauth/callback`
  - sandbox callback 완료
  - Spotify PKCE draft external callback 처리
  - 연결 성공 결과 확인
  - 다음 단계(`/platforms` 또는 `/pms`)로 이동
- `/pms`
  - 현재 사용자 기준 PMS import bootstrap 로드
  - 연결된 preferred platform의 sandbox playlist import 후보 표시
  - 선택한 playlist를 PMS로 import
  - seed track / artist / genre / playlist 입력
  - bootstrap track별 Spotify 오디오 특성 readiness 표시
- `/ems`
  - mood / energy / familiarity / limit 조정
  - `POST /api/v1/ems/workspace/analysis` 자동 호출
  - API 추천값을 현재 workspace에 바로 적용 가능
- `/gms-preview`
  - `POST /api/v1/gms/recommendations/preview` 호출
  - PMS/EMS workspace 값을 바탕으로 요청 생성
  - context, warnings, preview 결과 확인

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

1. sandbox PMS import를 실제 플랫폼 playlist API와 연결
2. EMS 신호에 최근 재생/행동 기반 값 추가
3. preview 응답을 실제 카탈로그 카드 UI와 연결
4. 공통 타입을 `packages/shared-types`로 옮길지 결정
5. 데스크탑 앱 재사용을 고려해 API 클라이언트 모듈 분리
