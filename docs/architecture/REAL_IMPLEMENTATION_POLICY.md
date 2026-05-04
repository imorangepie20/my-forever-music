# Real Implementation Policy

작성일: `2026-05-04`

이 프로젝트는 MacBook에서 먼저 시험 서비스를 만들고, 검증 후 Ubuntu 서버로 이전합니다. 이 과정에서 사용자 플로우에는 mock data, sandbox provider, 임시 데이터 경로를 기본값으로 노출하지 않습니다.

## 1. 원칙

- 실제 사용자 화면에 노출되는 기능은 실제 provider, 실제 API, 실제 저장소 기준으로 구현합니다.
- 아직 실 provider가 완성되지 않은 플랫폼은 UI에서 `PMS import 가능`으로 표시하지 않습니다.
- `나중에 구현` 전제의 mock 흐름은 제품 플로우에 연결하지 않습니다.
- 테스트 fixture는 테스트 코드 안에서만 사용합니다.
- local 개발 편의 기능은 실서비스 시험 경로와 분리합니다.

## 2. 현재 적용 기준

- PMS playlist import의 실제 구현 대상은 현재 `Spotify`입니다.
- 플랫폼 확장 순서는 `Spotify -> TIDAL -> YouTube Music`입니다.
- `TIDAL`은 다음 실제 provider 대상이지만, playlist provider와 Spotify audio feature 매칭 검증이 끝날 때까지 가입 기본 플랫폼과 PMS import 대상에서 제외합니다.
- `YouTube Music`은 TIDAL 안정화 이후 진행합니다.
- `Apple Music`은 Apple Developer 계정 준비 전까지 보류합니다.
- `Last.fm`은 스트리밍 구독 플랫폼이 아니라 청취 이력 signal source로만 연결합니다.
- Spotify OAuth 설정이 없으면 내부 sandbox 승인으로 대체하지 않고 명확히 실패시킵니다.
- MacBook 시험 서비스는 계정과 라이브러리가 재시작 후에도 남도록 DB 활성 프로필 사용을 우선합니다.

## 3. 허용되는 예외

- 단위 테스트와 컨트롤러 테스트의 fixture, fake client, sample response는 허용합니다.
- 외부 API 장애를 다루기 위한 fallback 로직은 허용하되, 사용자 데이터처럼 보이는 임의 생성값을 저장하지 않습니다.
- 특히 Spotify 오디오 특성은 공식 API 응답을 확보하지 못하면 `fallback_generated` 값을 만들지 않고 import를 중단합니다.
- 문서에는 planned provider를 적을 수 있지만, 구현 완료 전에는 사용자 선택지나 import 가능 상태로 노출하지 않습니다.

## 4. 새 기능 체크리스트

1. 실제 외부 API 또는 실제 저장소에 연결되어 있는가?
2. 사용자에게 보이는 데이터가 mock/sample/sandbox가 아닌가?
3. 실패 시 조용히 가짜 데이터로 대체하지 않고, 사용자와 로그에 정확히 드러나는가?
4. 테스트 fixture가 production bean이나 기본 프로필에 섞이지 않는가?
5. 문서의 구현 상태가 실제 코드와 같은가?
