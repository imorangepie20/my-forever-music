# EMS Acquisition Actual Runbook

작성: 2026-05-14

이 문서는 지금 로컬에서 그대로 실행할 EMS acquisition 검증 시나리오입니다. 값이 바뀌면 안 되는 secret은 출력하지 않습니다.

## 0. Stack 시작

터미널 1:

```bash
cd /Users/woosungjo/music-space/my-forever-music
./infra/scripts/restart-macbook-stack.sh
```

서비스 확인:

```bash
curl -fsS http://127.0.0.1:8000/health
curl -fsS http://127.0.0.1:8081/actuator/health
```

## 1. 수집에 쓸 user id 자동 선택

터미널 2:

```bash
cd /Users/woosungjo/music-space/my-forever-music

export API=http://127.0.0.1:8081
export AI=http://127.0.0.1:8000
pg() { docker exec -i my-forever-music-local-postgres psql -U postgres -d my_forever_music "$@"; }

export EMS_USER_ID="$(pg -Atc "
select a.user_id
from auth_user_account a
where exists (
  select 1
  from platform_account_credential c
  where c.user_id = a.user_id
    and c.platform_id in ('spotify', 'tidal')
)
order by a.registered_at desc
limit 1;
")"

echo "EMS_USER_ID=${EMS_USER_ID}"
```

통과 기준:
- `EMS_USER_ID=` 뒤에 값이 있어야 함

값이 비어 있으면:
1. `http://127.0.0.1:5173/platforms` 접속
2. Spotify 또는 TIDAL OAuth 연결
3. 위 `EMS_USER_ID` 명령 다시 실행

## 2. AI acquisition 모델 단독 확인

```bash
curl -fsS "$AI/v1/ems/acquisition/signals" \
  -H 'Content-Type: application/json' \
  -d '{
    "source_name": "Manual Smoke",
    "source_url": "https://pitchfork.com/info/rss/",
    "source_weight": 1.0,
    "max_signals": 5,
    "articles": [
      {
        "article_url": "https://pitchfork.com/reviews/tracks/",
        "title": "Best New Tracks",
        "summary": "Editorial track recommendations from Pitchfork.",
        "published_at": null
      }
    ]
  }'
```

통과 기준:
- HTTP 200
- 응답에 `"status":"ok"`
- 응답에 `"signals"` 배열 존재
- 각 signal의 `query`가 비어 있지 않음

실패하면:
- `AI_LLM_API_KEY`가 `services/ai/.env.local`에 있는지 확인
- `AI_EMS_ACQUISITION_MODEL`이 없으면 `AI_EMS_OVERVIEW_MODEL`이 있어야 함
- 실패를 우회하지 말고 AI 로그 확인:

```bash
tail -n 120 tmp/local-stack/logs/ai.log
```

## 3. 실제 source로 acquisition run 실행

기본 RSS source 5개를 실제 입력으로 사용합니다.

브라우저로 실행하려면:

```text
http://127.0.0.1:5173/ems/acquisition-admin
```

관리자 계정으로 접속한 뒤 `User ID`에 `${EMS_USER_ID}` 값을 넣고 기본 source를 그대로 두고 `실행`을 누릅니다.
반복 실행 시 이미 처리된 article URL과 이미 queue된 platform/query seed는 다시 넣지 않습니다.

터미널로 실행하려면:

```bash
curl -fsS "$API/api/v1/ems/acquisition/run" \
  -H 'Content-Type: application/json' \
  -d "{
    \"user_id\": \"${EMS_USER_ID}\",
    \"platforms\": [\"spotify\", \"tidal\"],
    \"sources\": [
      {
        \"name\": \"Pitchfork News\",
        \"type\": \"rss\",
        \"url\": \"https://pitchfork.com/feed/feed-news/rss\",
        \"weight\": 1.0
      },
      {
        \"name\": \"Pitchfork Track Reviews\",
        \"type\": \"rss\",
        \"url\": \"https://pitchfork.com/feed/feed-track-reviews/rss\",
        \"weight\": 1.1
      },
      {
        \"name\": \"Pitchfork Best New Tracks\",
        \"type\": \"rss\",
        \"url\": \"https://pitchfork.com/feed/reviews/best/tracks/rss\",
        \"weight\": 1.4
      },
      {
        \"name\": \"Stereogum\",
        \"type\": \"rss\",
        \"url\": \"https://www.stereogum.com/feed/\",
        \"weight\": 1.1
      },
      {
        \"name\": \"NME\",
        \"type\": \"rss\",
        \"url\": \"https://www.nme.com/?alt=rss\",
        \"weight\": 1.0
      }
    ],
    \"max_articles_per_source\": 10,
    \"max_signals_per_run\": 12,
    \"per_seed_limit\": 5
  }"
```

통과 기준:
- HTTP 200
- `status`가 `completed` 또는 `completed_with_failures`
- `run.signal_count > 0`
- `run.seed_count > 0`
- `run.pool_run_count > 0`
- 반복 실행 시 `skipped_article_count` 또는 `skipped_seed_count`가 증가할 수 있음
- `seeds[*].pool_run_id`가 존재

## 4. 방금 실행한 run DB 확인

```bash
export EMS_ACQ_RUN_ID="$(pg -Atc "
select ems_acquisition_run_id
from ems_acquisition_run
order by started_at desc
limit 1;
")"

echo "EMS_ACQ_RUN_ID=${EMS_ACQ_RUN_ID}"

pg -c "
select ems_acquisition_run_id, trigger_type, status,
       source_count, article_count, skipped_article_count,
       signal_count, seed_count, skipped_seed_count, pool_run_count,
       failed_source_count, failed_seed_count, message
from ems_acquisition_run
where ems_acquisition_run_id = ${EMS_ACQ_RUN_ID};
"

pg -c "
select source_name, signal_type, query, confidence_score, status
from ems_acquisition_signal
where ems_acquisition_run_id = ${EMS_ACQ_RUN_ID}
order by ems_acquisition_signal_id
limit 20;
"

pg -c "
select platform_id, query, status, ems_pool_ingest_run_id,
       result_playlist_count, result_track_count, last_error
from ems_acquisition_seed
where ems_acquisition_run_id = ${EMS_ACQ_RUN_ID}
order by ems_acquisition_seed_id
limit 30;
"
```

통과 기준:
- `ems_acquisition_signal.query`가 실제 음악 검색어로 들어감
- `ems_acquisition_seed.status='completed'`가 1개 이상
- provider 인증 실패가 있으면 `last_error`에 원인이 있어야 함

## 5. EMS POOL 연결 확인

```bash
pg -c "
select r.ems_pool_ingest_run_id, r.source_platform, r.search_query,
       r.collection_source, r.status,
       r.total_playlist_entries, r.total_track_entries,
       r.processed_playlist_entries, r.processed_track_entries,
       r.failed_entries
from ems_pool_ingest_run r
join ems_acquisition_seed s
  on s.ems_pool_ingest_run_id = r.ems_pool_ingest_run_id
where s.ems_acquisition_run_id = ${EMS_ACQ_RUN_ID}
order by r.ems_pool_ingest_run_id desc;
"
```

통과 기준:
- 모든 연결 run의 `collection_source='acquisition_pool'`
- `total_playlist_entries` 또는 `total_track_entries`가 1 이상

## 6. Background worker 반영 대기 후 EMS 본 테이블 확인

```bash
sleep 30

pg -c "
select r.ems_pool_ingest_run_id, r.status,
       count(e.ems_pool_entry_id) as entry_count,
       count(*) filter (where e.status = 'completed') as completed_count,
       count(*) filter (where e.status = 'failed') as failed_count
from ems_pool_ingest_run r
join ems_pool_entry e
  on e.ems_pool_ingest_run_id = r.ems_pool_ingest_run_id
join ems_acquisition_seed s
  on s.ems_pool_ingest_run_id = r.ems_pool_ingest_run_id
where s.ems_acquisition_run_id = ${EMS_ACQ_RUN_ID}
group by r.ems_pool_ingest_run_id, r.status
order by r.ems_pool_ingest_run_id desc;
"

pg -c "
select source_platform, count(*) as playlist_count
from ems_collected_playlist
where collection_source = 'acquisition_pool'
group by source_platform
order by source_platform;
"

pg -c "
select source_platform, count(*) as track_count
from ems_collected_track
where collection_source = 'acquisition_pool'
group by source_platform
order by source_platform;
"
```

통과 기준:
- pool entry의 `completed_count`가 증가
- `ems_collected_playlist` 또는 `ems_collected_track`에 `collection_source='acquisition_pool'` row가 생김
- 실패 entry가 있으면 `ems_pool_entry.last_error`에 원인이 남아야 함

실패 entry 상세:

```bash
pg -c "
select e.entry_type, e.source_platform, e.title, e.status, e.last_error
from ems_pool_entry e
join ems_acquisition_seed s
  on s.ems_pool_ingest_run_id = e.ems_pool_ingest_run_id
where s.ems_acquisition_run_id = ${EMS_ACQ_RUN_ID}
  and e.status = 'failed'
order by e.updated_at desc
limit 20;
"
```

## 7. UI에서 POOL 상태 확인

브라우저:

```text
http://127.0.0.1:5173/ems/pool-admin
```

확인:
- 최신 EMS POOL run이 보이는지
- `collection_source='acquisition_pool'` run의 처리 상태가 진행/완료되는지
- 실패 entry tooltip 또는 상세에서 원인이 보이는지

## 8. 실패 source 검증

잘못된 RSS source를 넣어서 fallback 없이 실패하는지 확인합니다.

```bash
curl -fsS "$API/api/v1/ems/acquisition/run" \
  -H 'Content-Type: application/json' \
  -d "{
    \"user_id\": \"${EMS_USER_ID}\",
    \"platforms\": [\"spotify\"],
    \"sources\": [
      {
        \"name\": \"Broken Feed\",
        \"type\": \"rss\",
        \"url\": \"https://127.0.0.1:1/not-found.xml\",
        \"weight\": 1.0
      }
    ],
    \"max_articles_per_source\": 5,
    \"max_signals_per_run\": 5,
    \"per_seed_limit\": 5
  }"
```

통과 기준:
- 응답 `status='failed'`
- `run.failed_source_count=1`
- `run.signal_count=0`
- `run.pool_run_count=0`
- mock/preview track이 생성되지 않음

## 9. 기존 EMS search 회귀 확인

```bash
curl -fsS "$API/api/v1/ems/collection/search" \
  -H 'Content-Type: application/json' \
  -d "{
    \"user_id\": \"${EMS_USER_ID}\",
    \"platform_id\": \"spotify\",
    \"query\": \"jazz\"
  }"
```

DB 확인:

```bash
pg -c "
select ems_pool_ingest_run_id, source_platform, search_query, collection_source, status
from ems_pool_ingest_run
order by created_at desc
limit 5;
"
```

통과 기준:
- 기존 검색 응답 `status='ems_search_pooled'`
- 방금 검색 run은 `collection_source='search_pool'`
- acquisition run은 `collection_source='acquisition_pool'`

## 10. 자동화 테스트

```bash
cd /Users/woosungjo/music-space/my-forever-music/services/api
./gradlew test
```

```bash
cd /Users/woosungjo/music-space/my-forever-music
services/ai/.venv/bin/python -m pytest services/ai/tests
```

통과 기준:
- API 전체 테스트 통과
- AI 전체 테스트 통과

## 최종 성공 판정

아래가 모두 맞으면 1차 EMS acquisition 기능은 실제 동작 기준으로 통과입니다.

- AI endpoint가 실제 GPT-compatible API key로 signal을 생성
- Pitchfork RSS source에서 signal이 저장됨
- signal이 Spotify/TIDAL search seed로 변환됨
- seed별 `ems_pool_ingest_run`이 생성됨
- acquisition POOL run은 `collection_source='acquisition_pool'`
- background worker가 EMS 본 테이블로 반영
- 실패는 source/seed/entry 단위 error로 남고 mock/fallback으로 숨겨지지 않음
