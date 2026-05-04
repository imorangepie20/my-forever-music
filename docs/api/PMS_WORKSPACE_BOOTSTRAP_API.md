# PMS Workspace Bootstrap API

작성일: `2026-04-30`

이 문서는 `apps/web`의 PMS 화면이 사용자 PMS 라이브러리와 플레이리스트 후보를 불러올 때 사용하는 공개 API 계약입니다.

## 목적

- import된 사용자 라이브러리가 있으면 playlist, track seed suggestion, artist/genre suggestion을 한 번에 내려주기
- import 전에는 가짜 playlist/track seed를 내려주지 않고 빈 라이브러리 상태를 명확히 반환하기
- 이후 실제 DB/플랫폼 연동이 확장되어도 프론트 계약은 유지하기

## 엔드포인트

- `GET /api/v1/pms/workspace/bootstrap`

## 요청

이 엔드포인트는 현재 요청 본문 없이 호출합니다.

- optional query parameter
  - `user_id`
  - `playlist_id`

## 응답

### 응답 구조

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
  - `cover_image_url`
  - `platform_external_url`
  - `platform_uri`
- `suggested_tracks`
  - `album_title`
  - `album_image_url`
  - `platform_external_url`
  - `platform_uri`
  - `preview_url`
  - `duration_ms`
  - `seed`
  - `spotify_track_id`
  - `spotify_audio_features_filled`
  - `spotify_audio_feature_source`
- `suggested_artists`
- `suggested_genres`

## 현재 구현 메모

- 현재는 `실제 사용자 데이터 우선` 소스 구조다
- `user_id`가 있고 해당 사용자의 정식 `PMS user library`가 있으면, 그 데이터가 가장 먼저 사용된다
- 그다음 같은 사용자의 raw import snapshot이 있으면, 그 데이터가 복구용 소스로 사용된다
- 그다음 `database` 같은 DB 활성 프로필에서는 같은 `user_id` 소유의 `pms_*` 테이블 데이터를 읽는다
- 어느 소스에도 사용자 데이터가 없으면 빈 playlist/track/signal 목록을 반환한다
- 현재 DB 스키마는 `playlist / track / playlist_track` 기반의 PMS 카탈로그를 제공하지만, 사용자 플로우에서 임의 demo 데이터를 fallback으로 노출하지 않는다
- 현재 `pms_track`는 track 메타데이터뿐 아니라 Spotify 오디오 특성 전체 스냅샷을 함께 저장하는 구조로 확장되었다
- 현재 `PMS user library`는 `pms_user_playlist / pms_user_track / pms_user_playlist_track` 또는 `local in-memory store`를 통해 유지된다
- Flyway 마이그레이션:
  - [V1__create_pms_bootstrap_catalog.sql](/Users/woosungjo/music-space/my-forever-music/services/api/src/main/resources/db/migration/V1__create_pms_bootstrap_catalog.sql)
- import 기반 사용자 라이브러리 마이그레이션:
  - [V12__create_pms_user_library_storage.sql](/Users/woosungjo/music-space/my-forever-music/services/api/src/main/resources/db/migration/V12__create_pms_user_library_storage.sql)
- DB 활성 시 같은 `user_id` 소유 playlist의 첫 항목을 기본 workspace playlist로 사용한다
- user library 기반 bootstrap에서는 가장 최근에 sync된 playlist를 기본 workspace playlist로 사용한다
- raw import 복구 bootstrap에서는 가장 최근에 PMS로 들여온 playlist를 기본 workspace playlist로 사용한다
- `playlist_id`가 같이 들어오면 가능한 경우 그 playlist를 현재 workspace 기준으로 우선 투영한다
- seed track은 `pms_playlist_track.is_seed = true` 기준으로 고른다
- artist / genre suggestion은 선택된 기본 playlist의 track 분포를 집계해 계산한다
- `suggested_tracks`는 현재 각 track이 Spotify 오디오 특성을 채운 상태인지 함께 내려준다
- 현재 응답은 플레이리스트 cover image, 트랙 album image, 외부 플랫폼 URL, platform URI까지 함께 내려주므로 `PMS / EMS / GMS` 페이지에서 같은 리치 미디어 컨텍스트를 재사용할 수 있다

## 다음 연결 지점

1. playlist owner를 실제 사용자 엔터티와 더 명확히 연결
2. 정식 user library와 raw import snapshot의 역할 분리를 더 명확히 문서화
3. seed suggestion을 저장된 청취 이력과 연결
4. PMS에서 선택한 값이 이후 EMS / GMS 호출과 사용자 저장 상태로 이어지게 확장
5. 빈 라이브러리 상태에서 사용자를 플랫폼 연결/import로 자연스럽게 안내하는 UX를 계속 개선
