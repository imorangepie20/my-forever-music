# PMS Track Audio Feature Storage

작성일: `2026-05-02`

## 목적

이 문서는 사용자의 구독 스트리밍 플랫폼에서 플레이리스트를 가져올 때, 각 트랙의 `Spotify 오디오 특성`을 어떤 기준으로 저장해야 하는지 정의합니다.

핵심 원칙은 간단합니다.

- `PMS`에 저장되는 트랙은 가능한 한 `Spotify Get Track's Audio Features` 기준의 전체 스냅샷을 가져야 합니다.
- 부분 저장이 아니라 `전체 필드 채움`을 기본 원칙으로 합니다.
- 직접 Spotify 오디오 특성을 확보하지 못한 트랙은 가짜 값으로 채우지 않고 import를 중단하거나 사용자에게 재시도/제외 정책을 안내합니다.

## 적용 범위

- 사용자 스트리밍 플랫폼 플레이리스트 import
- `PMS` 트랙 저장
- Spotify track 매칭과 오디오 특성 보강
- PMS bootstrap과 이후 EMS/GMS 추천의 기준 데이터

## 저장 원칙

### 플레이리스트 import 시점 저장

사용자의 스트리밍 플랫폼 플레이리스트를 가져올 때는 아래 순서를 따릅니다.

1. 플랫폼 원본 트랙 메타데이터를 확보합니다.
2. Spotify track 매칭을 시도합니다.
3. 매칭 성공 시 Spotify 오디오 특성을 가져옵니다.
4. 매칭 또는 오디오 특성 조회 실패 시 `PMS`에 임의 값을 저장하지 않습니다.
5. 최종적으로 `오디오 특성 전체 스냅샷`이 확보된 트랙만 `PMS`에 저장합니다.

즉, 목표 상태에서는 `PMS 트랙 저장`과 `오디오 특성 채움`이 분리된 선택 단계가 아니라 하나의 import 파이프라인으로 동작해야 합니다.

### 전체 스냅샷 기준

현재 프로젝트에서 `Spotify 오디오 특성 전체 스냅샷`으로 보는 필드는 아래와 같습니다.

- `spotify_track_id`
- `spotify_analysis_url`
- `spotify_track_href`
- `spotify_uri`
- `spotify_feature_type`
- `spotify_duration_ms`
- `spotify_key`
- `spotify_mode`
- `spotify_time_signature`
- `spotify_acousticness`
- `spotify_danceability`
- `spotify_energy`
- `spotify_instrumentalness`
- `spotify_liveness`
- `spotify_loudness`
- `spotify_speechiness`
- `spotify_tempo`
- `spotify_valence`
- `spotify_resolved_at`

추가 관리 필드:

- `spotify_audio_feature_source`
- `spotify_audio_features_filled`

## 상태 기준

### source 값 기준

`spotify_audio_feature_source`는 오디오 특성을 어떻게 확보했는지 표현합니다.

- `spotify_api`
  - Spotify API에서 직접 가져온 값
- `spotify_match`
  - 타 플랫폼 트랙을 Spotify track에 매칭한 뒤 가져온 값
- `unresolved`
  - 실제 import 목표 상태에서는 허용하지 않는 임시 상태

### filled 값 기준

`spotify_audio_features_filled`는 단순히 일부 값이 있다는 뜻이 아니라, 이 프로젝트가 요구하는 전체 스냅샷이 채워졌는지 나타내는 플래그입니다.

아래 조건을 만족해야 `true`로 봅니다.

- 전체 수치 필드가 모두 존재함
- `spotify_resolved_at`이 존재함
- source가 명확함

## 현재 구현 메모

현재 코드 기준 반영 내용:

- `pms_track` 테이블이 Spotify 오디오 특성 스냅샷 저장 구조로 확장됨
- Flyway 마이그레이션: [V3__add_spotify_audio_features_to_pms_track.sql](/Users/woosungjo/music-space/my-forever-music/services/api/src/main/resources/db/migration/V3__add_spotify_audio_features_to_pms_track.sql)
- JPA 모델: [PmsTrackSpotifyAudioFeatures.java](/Users/woosungjo/music-space/my-forever-music/services/api/src/main/java/io/myforevermusic/api/modules/pms/infrastructure/persistence/PmsTrackSpotifyAudioFeatures.java)
- 트랙 엔터티: [PmsCatalogTrackEntity.java](/Users/woosungjo/music-space/my-forever-music/services/api/src/main/java/io/myforevermusic/api/modules/pms/infrastructure/persistence/PmsCatalogTrackEntity.java)
- PMS bootstrap 응답은 track별 `spotify_audio_features_filled`, `spotify_audio_feature_source`를 같이 보여줌
- 실제 Spotify import provider는 `spotify_api` 응답이 누락되면 fallback 스냅샷을 만들지 않고 import를 실패시킴

## 다음 연결 지점

1. TIDAL provider에서 Spotify track 매칭을 실제 API 기반으로 추가
2. Spotify 오디오 특성 조회 실패 시 재시도/부분 제외/사용자 안내 정책 결정
3. import 시점의 `complete snapshot save` 검증 단계를 provider 공통 계약으로 강화
4. 이 정책을 실제 플랫폼 import API 계약과 연결
