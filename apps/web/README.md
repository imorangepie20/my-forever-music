# apps/web

`imapplepieTemplate001` 템플릿을 기반으로 가져온 뒤, `my-forever-music`에 맞게 최소 제품 셸로 재정리한 프론트엔드 앱입니다.

## 현재 포함된 것

- 최소 라우트 셸
- 음악 서비스용 메인 레이아웃
- `GMS recommendation preview` 테스트 화면
- Spring Boot API 호출 레이어
- 기존 템플릿에서 가져온 공통 HUD 스타일과 카드 컴포넌트

## 핵심 라우트

- `/`
  - 프로젝트 홈
  - API/AI 연결 상태와 현재 workspace 요약 표시
- `/platforms`
  - 스트리밍 플랫폼 카탈로그 로드
  - PMS import 기준 플랫폼 선택
  - Spotify 오디오 특성 기준과 fallback 전략 표시
- `/pms`
  - seed track / artist / genre / playlist 입력
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
- 로컬 Vite 개발에서는 `vite.config.ts`의 proxy가 `/api`, `/actuator`를 `http://localhost:8080`으로 전달합니다.
- 필요하면 `VITE_API_BASE_URL`로 별도 API 주소를 줄 수 있습니다.
- 문서 링크 주소를 따로 주고 싶으면 `VITE_API_DOCS_URL`, `VITE_AI_DOCS_URL`도 사용할 수 있습니다.

## 다음 추천 작업

1. PMS 입력을 실제 플레이리스트/라이브러리 API와 연결
2. EMS 신호에 최근 재생/행동 기반 값 추가
3. preview 응답을 실제 카탈로그 카드 UI와 연결
4. 공통 타입을 `packages/shared-types`로 옮길지 결정
5. 데스크탑 앱 재사용을 고려해 API 클라이언트 모듈 분리
