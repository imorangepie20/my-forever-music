# PMS Playlist Import API

작성일: `2026-05-03`

이 문서는 가입과 플랫폼 OAuth 연결 다음 단계로, `PMS`에 실제 사용자 플레이리스트를 가져오는 공개 API 계약을 정리합니다.

## 목적

- 연결된 스트리밍 플랫폼에서 가져올 플레이리스트 후보를 `PMS` 화면에 보여주기
- 선택한 플레이리스트를 `PMS`에 적재하기
- 적재 시점에 track metadata 저장을 먼저 보장하고, 오디오 특성 보강 상태를 함께 기록하기
- 적재한 플레이리스트를 정식 `PMS user library`로 동기화하고 바로 `PMS workspace bootstrap`에 반영되도록 연결하기

## 엔드포인트

- `GET /api/v1/pms/import/bootstrap`
- `POST /api/v1/pms/import/playlists`

## 요청

### import bootstrap

- query parameter
  - `user_id`

### import playlists

- body
  - `user_id`
  - `platform_id`
  - `external_playlist_ids`

## 응답

### import bootstrap 응답 구조

- `service`
- `status`
- `generated_at`
- `user`
  - `user_id`
  - `display_name`
  - `preferred_platform_id`
- `platform_connection`
  - `platform_id`
  - `display_name`
  - `connected`
  - `connection_mode`
  - `external_account_label`
  - `sync_ready`
  - `credential_status`
  - `reconnect_required`
- `summary`
  - `preferred_platform_connected`
  - `reconnect_required`
  - `available_playlist_count`
  - `imported_playlist_count`
  - `next_step_path`
  - `next_step_message`
- `available_playlists`
  - `external_playlist_id`
  - `title`
  - `source_platform`
  - `track_count`
  - `curator`
  - `description`
  - `cover_image_url`
  - `platform_external_url`
  - `platform_uri`
  - `already_imported`
  - `audio_feature_policy`
- `imported_playlists`
  - `playlist_id`
  - `external_playlist_id`
  - `title`
  - `source_platform`
  - `cover_image_url`
  - `platform_external_url`
  - `platform_uri`
  - `track_count`
  - `imported_at`

### import playlists 응답 구조

- `service`
- `status`
- `processed_at`
- `import_result`
  - `user_id`
  - `platform_id`
  - `platform_display_name`
  - `imported_playlist_count`
  - `imported_track_count`
  - `complete_audio_feature_track_count`
  - `complete_spotify_audio_feature_track_count`
  - `connection_mode`
  - `library_synced_playlist_count`
  - `library_synced_track_count`
- `playlists`
- `next_step`
  - `path`
  - `message`

## 현재 구현 메모

- 현재 `spotify`는 실제 OAuth access token이 있으면 `GET /me/playlists`, `GET /playlists/{playlist_id}/items`를 호출해 실사용자 플레이리스트를 읽는다
- 현재 `spotify` playlist summary는 cover image, external URL, playlist URI까지 함께 보존한다
- 현재 `spotify` track import는 album title, album image, external URL, track URI, preview URL까지 함께 보존한다
- 현재 코드 기준 PMS import provider는 `spotify`, `tidal`이 존재하며, 안정화 수준은 `spotify`가 더 높다
- provider 확장 순서는 `spotify -> tidal -> youtube-music`이며, `apple-music`은 Apple Developer 계정 준비 전까지 보류한다
- `tidal`, `youtube-music`, `apple-music`은 실제 provider 구현과 검증이 끝날 때까지 import 후보에 노출하지 않는다
- `local` 프로필에서는 import 저장소가 `in-memory`라 서버 재시작 시 초기화된다
- `database` 같은 DB 활성 프로필에서는 import 결과가 `pms_imported_playlist`, `pms_imported_track`, `pms_imported_playlist_track` 테이블에 저장된다
- 같은 적재 직후 정식 사용자 라이브러리도 함께 동기화되며, DB 활성 프로필에서는 `pms_user_playlist`, `pms_user_track`, `pms_user_playlist_track` 테이블에 저장된다
- `local` 프로필에서는 이 정식 사용자 라이브러리도 `in-memory` 저장소로 유지된다
- import 대상은 현재 `preferred platform` 기준으로만 노출된다
- `POST /api/v1/pms/import/playlists`는 연결된 플랫폼 상태가 있어야만 동작한다
- PMS import는 `platform credential + platform playlist provider`를 통해 후보 playlist를 읽는다
- `spotify` provider는 현재 `owned playlist`와 `collaborative playlist`만 import 대상으로 노출한다
- `spotify` credential이 만료되었거나 만료 60초 이내이면 import/bootstrap 직전에 refresh token으로 자동 갱신을 시도한다
- usable credential을 refresh로 복구하지 못하면 bootstrap은 `reconnect_required=true`로 응답하고 다음 단계를 `/platforms`로 되돌린다
- 같은 상태에서 `POST /api/v1/pms/import/playlists`를 호출하면 `409 conflict`와 `code=platform_reconnect_required`를 반환한다
- 현재 `spotify` provider는 audio-features 조회가 가능하면 그 값을 저장하고, 조회가 실패하면 import를 계속 진행하면서 `spotify_audio_features_filled=false` placeholder를 저장한다
- 현재 `tidal` provider는 TIDAL `ISRC`를 기준으로 ReccoBeats 후보를 고르고, 일치하는 항목이 없으면 import를 계속 진행하면서 placeholder를 저장한다
- 즉, 오디오 특성 누락은 더 이상 import 실패의 단일 조건이 아니다
- `audio_feature_policy`는 현재 `provider_neutral_enrichment`로 내려가며, import 후 외부 provider 보강을 전제로 한다
- `complete_audio_feature_track_count`는 현재 권장되는 provider-neutral 카운트 필드다
- `complete_spotify_audio_feature_track_count`는 legacy 호환 필드명이며, 현재는 `완전한 호환 스냅샷을 가진 track 수`로 해석한다
- 이 적재 결과는 먼저 `PMS user library`로 동기화되고, 이후 `GET /api/v1/pms/workspace/bootstrap?user_id=...`의 최우선 소스로 사용된다
- 적재 후 저장된 media metadata는 `PMS / EMS / GMS` 페이지의 공통 플레이어와 리치 카드 UI에 그대로 재사용된다
- 저장 기준 문서는 [PMS_TRACK_AUDIO_FEATURE_STORAGE.md](/Users/woosungjo/music-space/my-forever-music/docs/api/PMS_TRACK_AUDIO_FEATURE_STORAGE.md) 를 따른다

## 다음 연결 지점

1. 실제 TIDAL 계정으로 end-to-end import를 검증하고 응답 shape 차이를 수집
2. 정식 `PMS user library`와 이후 사용자 편집/평가 도메인을 어떻게 연결할지 정의
3. ReccoBeats 등 외부 공급원 기반 audio feature backfill 정책을 더 정교하게 설계
4. import 완료 후 사용자 행동 이벤트와 재학습 큐까지 연결
