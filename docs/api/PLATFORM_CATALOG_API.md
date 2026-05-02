# Platform Catalog API

## Endpoint

- `GET /api/v1/platforms/catalog`

## Purpose

이 엔드포인트는 사용자가 구독 중인 스트리밍 플랫폼을 선택하는 온보딩 시작점과, 각 플랫폼이 `PMS / EMS / GMS` 흐름에서 어떤 역할을 맡는지 설명하는 카탈로그를 제공합니다.

현재는 정적 카탈로그 응답이지만, 장기적으로는 다음 기준 문서와 연결됩니다.

- `PROJECT_KEY_SERVICE.md`
- 플랫폼별 PMS 적재 흐름
- Spotify 오디오 특성 기준과 fallback 특성 생성 전략
- playlist import 시 track별 Spotify 오디오 특성 전체 저장 요구사항

## Response Shape

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

## Notes

- 이 응답은 현재 `사용자 연결 상태`를 뜻하지 않습니다.
- 이 응답은 `플랫폼별 제품 역할`과 `구현 우선순위`를 설명하는 카탈로그입니다.
- 실제 OAuth 연결, PMS import job, EMS ingestion job은 이후 단계에서 추가됩니다.
