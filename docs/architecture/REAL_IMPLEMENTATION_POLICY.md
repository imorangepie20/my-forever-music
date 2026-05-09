# Real Implementation Policy

작성일: `2026-05-04`

이 프로젝트는 MacBook에서 먼저 시험 서비스를 만들고, 검증 후 Ubuntu 서버로 이전합니다. 이 과정에서 사용자 플로우에는 mock data, sandbox provider, 임시 데이터 경로를 기본값으로 노출하지 않습니다.

## 1. 원칙

- 실제 사용자 화면에 노출되는 기능은 실제 provider, 실제 API, 실제 저장소 기준으로 구현합니다.
- 아직 실 provider가 완성되지 않은 플랫폼은 UI에서 `PMS import 가능`으로 표시하지 않습니다.
- `나중에 구현` 전제의 mock 흐름은 제품 플로우에 연결하지 않습니다.
- 테스트 fixture는 테스트 코드 안에서만 사용합니다.
- local 개발 편의 기능은 실서비스 시험 경로와 분리합니다.
- 외부 API 권한 제약으로 사용할 수 없는 기능은 제품 목표에 있더라도 `현재 기본 사용자 경로`로 가정하지 않습니다.

## 2. 현재 적용 기준

- PMS playlist import의 실제 구현 대상은 현재 `Spotify`입니다.
- 플랫폼 확장 순서는 `Spotify -> TIDAL -> YouTube Music`입니다.
- `TIDAL`은 다음 실제 provider 대상이지만, playlist provider와 provider-neutral audio feature enrichment 검증이 끝날 때까지 가입 기본 플랫폼과 PMS import 대상에서 제외합니다.
- `YouTube Music`은 TIDAL 안정화 이후 진행합니다.
- `Apple Music`은 Apple Developer 계정 준비 전까지 보류합니다.
- `Last.fm`은 스트리밍 구독 플랫폼이 아니라 청취 이력 signal source로만 연결합니다.
- Spotify OAuth 설정이 없으면 내부 sandbox 승인으로 대체하지 않고 명확히 실패시킵니다.
- MacBook 시험 서비스는 계정과 라이브러리가 재시작 후에도 남도록 DB 활성 프로필 사용을 우선합니다.

## 3. 허용되는 예외

- 단위 테스트와 컨트롤러 테스트의 fixture, fake client, sample response는 허용합니다.
- 외부 API 장애를 다루기 위한 fallback 로직은 허용하되, 사용자 데이터처럼 보이는 임의 생성값을 저장하지 않습니다.
- 오디오 특성을 확보하지 못해도 가짜 수치를 만들지 않는 한 `metadata import + unresolved 저장`은 허용합니다.
- 특히 개인 개발 환경에서는 Spotify audio features를 필수 성공 경로로 두지 않습니다.
- 문서에는 planned provider를 적을 수 있지만, 구현 완료 전에는 사용자 선택지나 import 가능 상태로 노출하지 않습니다.

## 4. 전 프로젝트 오류 처리 원칙

이 원칙은 재생, 플랫폼 연동, PMS/EMS/GMS, 인증, 저장소, SSL, AI 서비스, 데스크탑 확장까지 모든 프로젝트 영역에 적용합니다.

- 오류는 반드시 실패한 경계와 근본 원인을 확인한 뒤 수정합니다.
- provider 호출 실패, 인증/권한 실패, 저장소 실패, SDK 초기화 실패, 네트워크/SSL 실패를 다른 플랫폼, preview 데이터, mock 데이터, timer, message suppression으로 바꾸지 않습니다.
- 사용자에게 재연결이나 재시도를 요구하기 전에 token refresh, provider account id 복구, scope, redirect URI, SSL, 실제 요청/응답을 먼저 검증합니다.
- 복잡한 통합 오류는 제품 전체 흐름에서 바로 추측 수정하지 않고, 문제가 난 provider/SDK/API 경계만 남긴 최소 재현 하네스나 격리 페이지를 먼저 만들어 실제 실패 조건을 확인합니다.
- 임시 방편이 꼭 필요한 운영 사고라면 제품 코드에 섞지 않고 별도 문서에 원인, 범위, 제거 조건, 만료 시점을 기록합니다.

## 5. 새 기능 체크리스트

1. 실제 외부 API 또는 실제 저장소에 연결되어 있는가?
2. 사용자에게 보이는 데이터가 mock/sample/sandbox가 아닌가?
3. 실패 시 조용히 가짜 데이터로 대체하지 않고, 사용자와 로그에 정확히 드러나는가?
4. 테스트 fixture가 production bean이나 기본 프로필에 섞이지 않는가?
5. 문서의 구현 상태가 실제 코드와 같은가?
6. 오류 처리가 우회/회피/임시 처리 대신 실패 경계와 근본 원인을 수정하는가?
