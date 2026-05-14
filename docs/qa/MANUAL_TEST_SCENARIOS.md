# Manual Test Scenarios

작성: 2026-05-12
범위: 최근 운영 도구 + 추천 모델 + 메타데이터 정규화 흐름. 재생은 **TIDAL 기본**으로 가정.

## 환경 준비

- `./infra/scripts/restart-macbook-stack.sh` 로 풀 스택 재시작 (DB → AI → Spring API → Vite Web)
- 관리자 계정 `jowoosungtidal@gmail.com` 로 로그인
- `/platforms` 에서 **TIDAL OAuth 연결** + **preferred platform = TIDAL**
- TIDAL 카탈로그에서 짧은 곡(2~3분) 하나를 미리 PMS 라이브러리에 import 해 두면 시나리오 1~3 검증이 빠름

---

## 시나리오 1 — TIDAL 자연 종료 → `play_completed`

목적: 트랙 끝까지 듣기가 `user_music_event` 에 적재되는지

1. `/pms` 또는 `/ems` playlist detail 에서 짧은 TIDAL 트랙을 재생
2. 끝까지 듣기 (skip 누르지 말 것)
3. 검증:
   - API 로그 (`tmp/local-stack/logs/api.log`) 에 `POST /api/v1/recommendations/events ... event_type=play_completed`
   - DB 활성 프로필이면 `user_music_event` 테이블에 `event_type='play_completed'`, `source_space='player'`, `source_platform='tidal'`, `position_ms ≈ duration_ms` 인 row
   - 직후 자동으로 다음 트랙이 시작되면 `play_started` 도 따라옴

## 시나리오 2 — TIDAL repeat-one `replay`

목적: 같은 트랙 자동 재시작이 `replay` event 로 적재되는지

1. PlaybackDock 의 repeat 버튼을 두 번 눌러 `repeat = one`
2. TIDAL 트랙 재생 → 끝까지 듣기
3. 같은 트랙이 자동 재시작
4. 검증: 같은 트랙에 대해 연속으로
   - `play_completed` (position ≈ duration)
   - `replay` (position = 0)
   두 event 가 `user_music_event` 에 적재

## 시나리오 3 — EMS / PMS detail like 버튼 → `track_saved`

1. `/ems` 에서 platform=TIDAL, query=`jazz` 검색 → 결과 playlist 카드 클릭 → detail 진입
2. track row 우측 하트 아이콘 클릭
3. 검증:
   - 하트가 채워지고 비활성화 (중복 차단)
   - `user_music_event` 에 `event_type='track_saved'`, `source_space='ems'`, `source_platform='tidal'` 한 row
4. EMS 본 collection playlist detail (검색이 아닌 적재된 playlist) 에서도 같은 동작
5. `/pms` playlist detail 에서도 → `source_space='pms'`

## 시나리오 4 — GMS Preview 6축 evidence + TIDAL 재생

1. `/gms-preview` 에서 mood / familiarity_bias 설정 후 Preview 요청 (PMS 라이브러리에 TIDAL 트랙이 있어야 candidate 가 잡힘)
2. 각 후보 카드 아래 axis evidence 패널이 6줄 노출되는지
3. 검증:
   - 점수 high(≥0.7) chip 은 primary 색, mid 는 기본, low(<0.4)는 amber
   - summary 가 한국어로 노출
   - 점수가 모두 null 이면 패널 자체가 안 보임
4. 카드 Play 버튼 → TIDAL playable target resolve 후 재생

## 시나리오 5 — Sidebar 스크롤 (admin)

1. admin 로그인 후 sidebar 메뉴 총 11개 (workspace 7 + admin 4)
2. 노트북 작은 viewport (약 800px 높이) 에서 nav 가 viewport 끝에 닿으면 **휠 스크롤 가능**
3. footer 의 Flow 카드 + Collapse 버튼은 항상 viewport 안에 보임

## 시나리오 6 — Playlist Quality admin

1. 일반 사용자로 `/recommendations/quality-admin` 진입 → **차단 화면**
2. admin 으로 같은 경로 → 헤더 + 6축 평균 카드 + 최근 추천 playlist 표
3. admin 으로 `/gms-preview` 에서 추천 한두 번 실행 → quality-admin 새로고침 시 `recommendation_id` 가 추가됨

## 시나리오 7 — SASRec Model registry promote / disable / rollback

전제: 시나리오 8 로 학습된 model artifact 가 최소 2개

1. `/recommendations/sasrec-admin` → Active Model 카드 확인
2. **Promote**: model_version 입력 후 Promote → ConfirmDialog → Active Model 갱신
3. 다른 model_version Promote → 직전 모델이 history 로 이동
4. **Rollback**: Rollback → 직전 모델로 복원
5. **Disable**: 현재 active model 을 Disable → history 직전 모델로 자동 교체. history 비면 active None

## 시나리오 8 — SASRec Auto-Train (수동 + auto-promote)

전제: admin user 에 `user_music_event` 30개 이상 (시나리오 1~3 반복 또는 수동 PMS save)

1. `/recommendations/sasrec-admin` → Auto-Train 클릭 → ConfirmDialog
2. 30초~몇 분 대기 (로컬 학습)
3. 검증:
   - 결과 카드에 qualified / promoted / Hit@K Δ + model_version 노출
   - qualified=true 면 Active Model 카드가 새 model 로 자동 promote
   - qualified=false 면 reason 표시, promote 안 됨
4. 다른 사용자를 검증하려면 Other user lookup 에 target `user_id` 입력 → **Train Target** → ConfirmDialog
5. 검증:
   - 결과 카드 `User` 가 target user 로 표시됨
   - target user lookup 의 `Latest train` / metric table 이 갱신됨
   - admin Active Model 카드는 target user 학습만으로 바뀌지 않음

## 시나리오 9 — SASRec Auto-Train scheduler (DB 영속)

활성화 설정 (`services/api/src/main/resources/application.yml` 또는 env):

```yaml
app:
  recommendation:
    sasrec:
      auto-train:
        enabled: true
        active-window-hours: 168
        min-event-delta: 50
        fixed-delay-ms: 300000
        initial-delay-ms: 30000
```

1. 설정 후 API 재시작
2. 약 30초 후 첫 tick — API 로그에 `SASRec auto-train tick user=... qualified=... summary=...`
   - `user-id` 를 비우면 최근 활성 일반 사용자도 target 으로 학습 가능해야 함
3. DB 활성 프로필이면 `sasrec_auto_train_log` 에 row 1개 이상 (`trained_at`, `event_count_at_train`, `qualified`, `promoted`)
4. 5분 후 다음 tick — 새 event 가 50개 미만이면 `skip user=... (delta=... < threshold=50)` 로그
5. **재시작 후 드리프트 유지**: API 재시작 → 또 30초 후 tick → 새 row 가 안 생기거나 skip 출력 (DB store 기반 직전 학습 시점 인식)

## 시나리오 10 — Metadata Normalization: MusicBrainz lookup + persist

1. `/recommendations/metadata-admin` 진입
2. title=`Bohemian Rhapsody`, artist=`Queen`, limit=`10`, **Persist=on** → Lookup
3. 검증:
   - 상단 후보 표에 5~10개 recording (mbid / title / artist / score / isrcs)
   - 하단 candidate 목록 status=pending 에 mbid + isrc candidate 자동 저장됨
   - status 필터 토글 (Pending / Accepted / Rejected / All) 정상 작동
4. TIDAL 트랙 ISRC 추적 사용 사례: PMS detail 에서 TIDAL 트랙의 title/artist 를 복사해 lookup → 후보 ISRC 가 PMS 트랙 기존 ISRC 와 매칭되는지 수동 비교
5. MusicBrainz rate limit (1 req/sec) — 빠르게 여러 번 누르면 429 가능. 로그에 `BAD_GATEWAY` 가 자주 떠야 함

## 시나리오 11 — Candidate Accept / Reject / Auto-accept

1. 시나리오 10 이후 pending candidate 가 있는 상태
2. row 의 **Accept** → ConfirmDialog → status=accepted, `resolved_by/at` 갱신
3. 다른 row 의 **Reject** → status=rejected
4. **Auto-accept**:
   - `min=0.95` 기본 → MusicBrainz 점수가 보통 < 0.95 라 대부분 skip. notice: `threshold 0.95 — reviewed N, accepted 0, skipped N`
   - `min=0.5` 로 낮춰서 다시 → accepted 증가
   - source 또는 candidate_kind 필터를 endpoint 호출 시 직접 query parameter 로 시험 (현재 UI 는 미노출)

---

## 회귀 검증 포인트 (이전 commit 영향 확인)

- **EMS pool admin**: `/ems/pool-admin` 에서 TIDAL 검색 후 적재 → 진행률 진행 → run 삭제 + 빈 playlist 일괄 정리
- **TIDAL playlist detail 재생**: PMS / EMS detail Play All → 큐 진입 → TIDAL playable target resolve 후 재생, 다음 트랙 자동 전환
- **공통 플레이어**: 페이지 이동해도 재생 유지, queue count 표시 (PlaybackDock)
- **온보딩 1사이클**: 로그인 → preferred platform = TIDAL → playlist import → EMS 검색 → GMS preview → 추천 → TIDAL 재생 완주

---

## 검증 도구 빠른 참조

| 영역 | 도구 |
|---|---|
| API 로그 | `tail -F tmp/local-stack/logs/api.log` |
| AI 로그 | `tail -F tmp/local-stack/logs/ai.log` |
| Web 로그 | `tail -F tmp/local-stack/logs/web.log` |
| DB 접속 | `docker exec -it my-forever-music-local-postgres psql -U postgres -d my_forever_music` |
| event 조회 예시 | `select event_type, count(*) from user_music_event where user_id=:u group by event_type;` |
| auto-train log 조회 | `select user_id, trained_at, qualified, promoted, summary from sasrec_auto_train_log order by trained_at desc limit 20;` |
| candidate 조회 | `select id, query_title, candidate_kind, candidate_value, candidate_score, status from track_identity_candidate order by created_at desc limit 30;` |
