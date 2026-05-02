# PMS Workspace Bootstrap API

작성일: `2026-04-30`

이 문서는 `apps/web`의 PMS 화면이 초기 seed 데이터와 플레이리스트 후보를 불러올 때 사용하는 bootstrap 계약입니다.

## 목적

- PMS 화면이 완전 수동 입력 상태를 벗어나도록 하기
- 플레이리스트 후보, track seed suggestion, artist/genre suggestion을 한 번에 내려주기
- 이후 실제 DB/플랫폼 연동으로 바뀌더라도 프론트 계약은 먼저 고정하기

## 엔드포인트

- `GET /api/v1/pms/workspace/bootstrap`

## 응답 구조

- `service`
- `status`
- `generated_at`
- `workspace_defaults`
  - `user_id`
  - `playlist_id`
  - `seed_track_ids`
  - `seed_artist_names`
  - `seed_genres`
- `playlists`
- `suggested_tracks`
  - `spotify_track_id`
  - `spotify_audio_features_filled`
  - `spotify_audio_feature_source`
- `suggested_artists`
- `suggested_genres`

## 현재 성격

- 현재는 `2단계 소스` 구조다
- `local` 프로필에서는 Spring Boot 내부 fallback bootstrap 데이터를 사용한다
- `database` 같은 DB 활성 프로필에서는 `PostgreSQL + Flyway`로 준비된 `pms_*` 테이블에서 실제 데이터를 읽는다
- 현재 DB 스키마는 `playlist / track / playlist_track` 기반의 최소 PMS 카탈로그를 제공한다
- 현재 `pms_track`는 track 메타데이터뿐 아니라 Spotify 오디오 특성 전체 스냅샷을 함께 저장하는 구조로 확장되었다

## 현재 구현 메모

- Flyway 마이그레이션:
  - [V1__create_pms_bootstrap_catalog.sql](/Users/woosungjo/music-space/my-forever-music/services/api/src/main/resources/db/migration/V1__create_pms_bootstrap_catalog.sql)
  - [V2__seed_demo_pms_bootstrap_catalog.sql](/Users/woosungjo/music-space/my-forever-music/services/api/src/main/resources/db/migration/V2__seed_demo_pms_bootstrap_catalog.sql)
- DB 활성 시 playlist의 첫 항목을 기본 workspace playlist로 사용한다
- seed track은 `pms_playlist_track.is_seed = true` 기준으로 고른다
- artist / genre suggestion은 선택된 기본 playlist의 track 분포를 집계해 계산한다
- `suggested_tracks`는 현재 각 track이 Spotify 오디오 특성을 채운 상태인지 함께 내려준다

## 다음 연결 지점

1. playlist owner를 실제 사용자 엔터티와 연결
2. seed suggestion을 저장된 청취 이력과 연결
3. PMS에서 선택한 값이 이후 EMS / GMS 호출과 사용자 저장 상태로 이어지게 확장
4. 현재 demo seed migration을 실제 ingest/관리 흐름으로 교체
