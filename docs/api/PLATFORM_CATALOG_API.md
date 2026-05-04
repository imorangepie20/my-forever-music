# Platform Catalog API

작성일: `2026-05-02`

이 문서는 사용 가능한 스트리밍 플랫폼과 각 플랫폼의 제품 역할을 설명하는 공개 API 계약입니다.

## 목적

- 사용자 온보딩 시작점에서 플랫폼 선택 목록 제공
- 각 플랫폼이 `PMS / EMS / GMS` 흐름에서 어떤 역할을 맡는지 설명
- 이후 실제 OAuth 연결과 import job이 생겨도 제품 기준 카탈로그 계약을 먼저 고정

## 엔드포인트

- `GET /api/v1/platforms/catalog`

## 요청

이 엔드포인트는 현재 요청 본문 없이 호출합니다.

## 응답

### 응답 필드

- `service`
- `status`
- `generated_at`
- `primary_audio_feature_source`
- `onboarding_flow`
- `platforms`
  - `platform_id`
  - `display_name`
  - `integration_stage`
  - `pms_import_supported`
  - `ems_collection_supported`
  - `audio_feature_strategy`
  - `pms_role`
  - `ems_role`
  - `notes`

### 예시 응답

```json
{
  "service": "api",
  "status": "ok",
  "generated_at": "2026-05-03T00:00:00Z",
  "primary_audio_feature_source": "spotify",
  "onboarding_flow": [
    "사용자가 구독 중인 스트리밍 플랫폼을 선택한다.",
    "선택한 플랫폼의 플레이리스트를 PMS로 가져온다.",
    "Spotify 오디오 특성을 확보하지 못한 track은 가짜 값으로 채우지 않고 import를 중단한다."
  ],
  "platforms": [
    {
      "platform_id": "spotify",
      "display_name": "Spotify",
      "integration_stage": "priority-analysis-source",
      "pms_import_supported": true,
      "ems_collection_supported": true,
      "audio_feature_strategy": "native-audio-features",
      "pms_role": "사용자 플레이리스트 PMS 적재의 1차 기준 플랫폼",
      "ems_role": "공개 플레이리스트와 트렌드 수집의 우선 연구 대상",
      "notes": [
        "핵심 오디오 특성 기준 소스"
      ]
    },
    {
      "platform_id": "tidal",
      "display_name": "TIDAL",
      "integration_stage": "planned-provider-next",
      "pms_import_supported": false,
      "ems_collection_supported": true,
      "audio_feature_strategy": "disabled-until-real-provider",
      "pms_role": "Spotify 다음으로 실제 TIDAL provider 구현 예정",
      "ems_role": "트렌딩 및 공개 플레이리스트를 EMS 후보군으로 수집하는 다음 대상",
      "notes": [
        "현재 사용자 온보딩에서는 선택할 수 없음",
        "TIDAL OAuth 2.1 + PKCE 토큰 교환 기반을 먼저 준비",
        "실제 TIDAL API playlist provider와 Spotify 특성 매칭 검증이 끝난 뒤 활성화"
      ]
    },
    {
      "platform_id": "last-fm",
      "display_name": "Last.fm",
      "integration_stage": "analysis-signal-source",
      "pms_import_supported": false,
      "ems_collection_supported": true,
      "audio_feature_strategy": "scrobble-history-signal",
      "pms_role": "플레이리스트 import보다 장기 청취 이력 신호에 집중",
      "ems_role": "scrobble, top artist, tag 데이터를 EMS/GMS 학습 신호로 연결",
      "notes": [
        "현재 단계에서는 PMS playlist import 대상이 아님",
        "장기 affinity 모델과 재생 이력 분석에 활용 예정"
      ]
    }
  ]
}
```

## 현재 구현 메모

- 이 응답은 현재 `사용자 연결 상태`를 뜻하지 않습니다.
- 이 응답은 `플랫폼별 제품 역할`과 `구현 우선순위`를 설명하는 카탈로그입니다.
- 현재 사용자 가입/온보딩에서 PMS import source로 선택 가능한 플랫폼은 `Spotify`뿐입니다.
- 확장 우선순위는 `Spotify -> TIDAL -> YouTube Music`이며, `Apple Music`은 개발자 계정 준비 전까지 보류합니다.
- `TIDAL`, `YouTube Music`, `Apple Music`은 실제 provider가 완성되기 전까지 `pms_import_supported=false`로 내려줍니다.
- `Last.fm`은 PMS import보다 장기 청취 분석 신호용 플랫폼으로 취급합니다.
- 이 문서는 [PROJECT_KEY_SERVICE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_KEY_SERVICE.md) 와 [PMS_TRACK_AUDIO_FEATURE_STORAGE.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_TRACK_AUDIO_FEATURE_STORAGE.md) 를 구현 계약 관점에서 연결합니다.

## 다음 연결 지점

1. 사용자별 플랫폼 연결 상태와 권한 범위를 응답에 결합
2. PMS import job 시작용 API와 연결
3. 플랫폼별 트랙 매칭 전략과 실패 처리 기준을 별도 문서로 분리
