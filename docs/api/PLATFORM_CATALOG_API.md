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
    "선택한 플랫폼의 플레이리스트를 PMS로 가져온다."
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
    }
  ]
}
```

## 현재 구현 메모

- 이 응답은 현재 `사용자 연결 상태`를 뜻하지 않습니다.
- 이 응답은 `플랫폼별 제품 역할`과 `구현 우선순위`를 설명하는 카탈로그입니다.
- 실제 OAuth 연결, PMS import job, EMS ingestion job은 이후 단계에서 추가됩니다.
- 이 문서는 [PROJECT_KEY_SERVICE.md](/Users/woosungjo/music-space/my-forever-music/docs/PROJECT_KEY_SERVICE.md) 와 [PMS_TRACK_AUDIO_FEATURE_STORAGE.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_TRACK_AUDIO_FEATURE_STORAGE.md) 를 구현 계약 관점에서 연결합니다.

## 다음 연결 지점

1. 사용자별 플랫폼 연결 상태와 권한 범위를 응답에 결합
2. PMS import job 시작용 API와 연결
3. 플랫폼별 트랙 매칭 전략과 실패 처리 기준을 별도 문서로 분리
