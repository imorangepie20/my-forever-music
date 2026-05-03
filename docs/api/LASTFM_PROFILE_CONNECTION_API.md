# Last.fm Profile Connection API

작성일: `2026-05-04`

## 목적

`Last.fm` 공개 사용자명을 계정에 저장하고, 이 값을 `Platforms bootstrap`과 `EMS analysis`에서 재사용하기 위한 API입니다.

이 경로는 OAuth 연결이 아니라 `analysis signal profile` 연결입니다. 즉, `PMS import`용 플랫폼 연결이 아니라 `장기 청취/affinity 신호` 연결을 저장하는 역할입니다.

## 엔드포인트

- `POST /api/v1/platforms/lastfm/profile`

## 요청

### Body

```json
{
  "user_id": "user-001",
  "username": "mibeen"
}
```

## 응답

### 예시

```json
{
  "service": "api",
  "status": "connected",
  "processed_at": "2026-05-04T03:40:00Z",
  "connection": {
    "user_id": "user-001",
    "platform_id": "last-fm",
    "display_name": "Last.fm",
    "connected": true,
    "connection_status": "connected",
    "connection_mode": "public-profile",
    "external_account_label": "mibeen",
    "scope_summary": "recent-scrobbles-read, top-artists-read, top-tracks-read",
    "sync_ready": false,
    "connected_at": "2026-05-04T03:40:00Z"
  },
  "next_step": {
    "path": "/platforms",
    "message": "Last.fm signal profile saved. You can use it for EMS analysis or choose another PMS import source."
  }
}
```

## 현재 구현 메모

- 이 API는 먼저 `Last.fm public profile` 조회로 사용자명을 검증한 뒤 저장합니다.
- 저장되면 `auth_user_account.last_fm_username`, `auth_user_account.last_fm_connected_at`도 함께 갱신됩니다.
- 동시에 플랫폼 연결 상태에는 `last-fm / public-profile` 연결이 생겨서 `/platforms` bootstrap에서 다시 보이게 됩니다.
- `sync_ready`는 `false`로 유지됩니다. `Last.fm`은 현재 PMS playlist import 소스가 아니라 분석 신호 소스이기 때문입니다.
- 이후 `POST /api/v1/platforms/lastfm/scrobbles/sync`가 이 저장된 사용자명을 기준으로 최근 scrobble snapshot을 적재합니다.
- `POST /api/v1/ems/workspace/analysis`와 `POST /api/v1/gms/recommendations/preview`는 저장된 scrobble snapshot이 있으면 그 artist recurrence를 먼저 blend 하고, 비어 있으면 live `Last.fm top artists` 조회로 fallback 합니다.

## 다음 연결 지점

1. Last.fm 사용자명을 세션/프로필 편집 화면에서도 수정 가능하게 확장
2. Last.fm 최근 scrobble을 주기 배치로 저장
3. Spotify 매칭을 거쳐 Last.fm track 신호를 오디오 특성 파이프라인과 결합
