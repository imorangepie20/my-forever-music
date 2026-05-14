# EMS Acquisition Test Scenarios

작성: 2026-05-14
범위: 음악 블로그/잡지/스트리밍 editorial source 기반 EMS playlist/track acquisition pipeline.

목표는 source article/feed -> AI signal extraction -> provider search resolve -> `ems_pool_*` -> EMS collected playlist/track 반영까지 확인하는 것입니다.

## 환경 준비

- 로컬 stack 실행: `./infra/scripts/restart-macbook-stack.sh`
- AI service env:
  - `AI_LLM_API_KEY` 설정 필수
  - `AI_EMS_ACQUISITION_MODEL`이 있으면 사용
  - 없으면 `AI_EMS_OVERVIEW_MODEL`을 acquisition 모델로 재사용
- API env:
  - `EMS_ACQUISITION_USER_ID` 또는 `EMS_DISCOVERY_USER_ID` 설정
  - 해당 user가 `/platforms`에서 Spotify 또는 TIDAL OAuth 연결 완료
- 로그:
  - API: `tail -F tmp/local-stack/logs/api.log`
  - AI: `tail -F tmp/local-stack/logs/ai.log`
- DB:
  - `docker exec -it my-forever-music-local-postgres psql -U postgres -d my_forever_music`

주의: API key, OAuth token, provider credential 값은 로그나 문서에 출력하지 않습니다.

---

## 시나리오 1 — AI acquisition 모델 단독 검증

목적: FastAPI AI service가 editorial article을 provider-searchable signal로 변환하는지 확인.

1. AI service가 실행 중인지 확인
2. 아래 요청 실행:

```bash
curl -s http://127.0.0.1:8000/v1/ems/acquisition/signals \
  -H 'Content-Type: application/json' \
  -d '{
    "source_name": "Manual QA Source",
    "source_url": "https://example.test/feed",
    "source_weight": 1.0,
    "max_signals": 5,
    "articles": [
      {
        "article_url": "https://example.test/a",
        "title": "The best new tracks this week",
        "summary": "A roundup of new songs, artists, and playlists.",
        "published_at": null
      }
    ]
  }'
```

검증:
- HTTP 200
- `status="ok"`
- `model` 값 존재
- `signals[0].query`가 비어 있지 않음
- `signals[*].query_variants`가 배열이며 각 signal당 최대 3개
- `signals[*].signal_type`은 `track | artist | playlist_query | genre | scene` 중 하나
- `confidence_score`는 `0.0 <= score <= 1.0`

실패 기준:
- 모델 미설정이면 HTTP 503과 `EMS acquisition model is not configured...`
- LLM 응답 JSON shape가 틀리면 HTTP 502
- 이 경우 mock/fallback 없이 실패해야 정상

---

## 시나리오 2 — 수동 acquisition run: RSS/Atom source -> EMS POOL

목적: API가 RSS/Atom source를 읽고 AI signal을 만든 뒤 provider search 결과를 `ems_pool_*`에 적재하는지 확인.

1. provider 연결이 있는 user id를 정한다.
2. 아래 요청 실행. `user_id`는 실제 user id로 교체한다.

```bash
curl -s http://127.0.0.1:8080/api/v1/ems/acquisition/run \
  -H 'Content-Type: application/json' \
  -d '{
    "user_id": "user-001",
    "platforms": ["spotify", "tidal"],
    "sources": [
      {
        "name": "Manual QA RSS",
        "type": "rss",
        "url": "https://example.com/feed.xml",
        "weight": 1.0
      }
    ],
    "max_articles_per_source": 10,
    "max_signals_per_run": 10,
    "per_seed_limit": 5
  }'
```

검증:
- HTTP 200
- `run.status`는 `completed` 또는 일부 source/provider 실패가 있으면 `completed_with_failures`
- `run.article_count > 0`
- `run.signal_count > 0`
- `run.seed_count > 0`
- `run.pool_run_count > 0`
- `signals[*].query`가 provider 검색 가능한 문자열
- `seeds[*].pool_run_id`가 존재

DB 검증:

```sql
select ems_acquisition_run_id, status, article_count, skipped_article_count,
       signal_count, seed_count, skipped_seed_count, pool_run_count,
       failed_source_count, failed_seed_count, started_at, completed_at
from ems_acquisition_run
order by started_at desc
limit 5;

select source_name, signal_type, query, confidence_score, status
from ems_acquisition_signal
order by created_at desc
limit 20;

select platform_id, query, status, ems_pool_ingest_run_id, result_playlist_count, result_track_count, last_error
from ems_acquisition_seed
order by created_at desc
limit 20;

select ems_pool_ingest_run_id, source_platform, search_query, collection_source, status,
       total_playlist_entries, total_track_entries
from ems_pool_ingest_run
where collection_source = 'acquisition_pool'
order by created_at desc
limit 20;
```

통과 기준:
- `ems_acquisition_seed.ems_pool_ingest_run_id`가 `ems_pool_ingest_run`과 연결됨
- 연결된 pool run의 `collection_source='acquisition_pool'`
- provider 인증 실패가 있으면 `last_error`에 원인이 남고 숨겨지지 않음

---

## 시나리오 2-B — source preset 확대 run

목적: 운영자가 기본 source 직접 입력 없이 확대 preset으로 EMS pool 수집량을 키울 수 있는지 확인.

1. preset 목록 확인:

```bash
curl -s http://127.0.0.1:8080/api/v1/ems/acquisition/source-presets
```

2. `editorial-expanded` preset으로 실행:

```bash
curl -s http://127.0.0.1:8080/api/v1/ems/acquisition/run \
  -H 'Content-Type: application/json' \
  -d '{
    "user_id": "user-001",
    "platforms": ["spotify", "tidal"],
    "source_preset": "editorial-expanded"
  }'
```

검증:
- preset 목록에 `editorial-expanded`가 있고 `source_count`가 기본 configured preset보다 큼
- run 응답의 `run.source_count`가 기본 12보다 큼
- `/ems/acquisition-admin`에서 같은 preset 선택 시 collection target이 기본값보다 커짐
- 실패 source가 있으면 `failed_source_count`, `message`, `last_error` 중 하나로 운영자에게 노출됨

---

## 시나리오 3 — POOL worker -> EMS 본 테이블 반영

목적: acquisition으로 생성된 `ems_pool_entry`가 background worker를 통해 EMS collected playlist/track으로 반영되는지 확인.

1. 시나리오 2 실행 후 10~30초 대기
2. `/ems/pool-admin`에서 최신 run 확인
3. DB 확인:

```sql
select r.ems_pool_ingest_run_id, r.status,
       count(e.ems_pool_entry_id) as entry_count,
       count(*) filter (where e.status = 'completed') as completed_count,
       count(*) filter (where e.status = 'failed') as failed_count
from ems_pool_ingest_run r
join ems_pool_entry e on e.ems_pool_ingest_run_id = r.ems_pool_ingest_run_id
where r.collection_source = 'acquisition_pool'
group by r.ems_pool_ingest_run_id, r.status
order by r.ems_pool_ingest_run_id desc
limit 10;

select collection_source, source_platform, count(*) as playlist_count
from ems_collected_playlist
where collection_source = 'acquisition_pool'
group by collection_source, source_platform;

select collection_source, source_platform, count(*) as track_count
from ems_collected_track
where collection_source = 'acquisition_pool'
group by collection_source, source_platform;
```

통과 기준:
- pool entry가 `completed`로 증가
- `ems_collected_playlist` 또는 `ems_collected_track`에 `collection_source='acquisition_pool'` row 생성
- playlist entry 처리 중 track 조회 실패가 있으면 해당 entry 또는 run에 error가 남음

---

## 시나리오 4 — 실패 source 처리

목적: 잘못된 source, 네트워크 실패, invalid XML이 실패로 기록되고 mock 데이터로 진행되지 않는지 확인.

요청:

```bash
curl -s http://127.0.0.1:8080/api/v1/ems/acquisition/run \
  -H 'Content-Type: application/json' \
  -d '{
    "user_id": "user-001",
    "platforms": ["spotify"],
    "sources": [
      {
        "name": "Broken Feed",
        "type": "rss",
        "url": "https://127.0.0.1:1/not-found.xml",
        "weight": 1.0
      }
    ],
    "max_articles_per_source": 5,
    "max_signals_per_run": 5,
    "per_seed_limit": 5
  }'
```

검증:
- HTTP 200
- `run.status="failed"` 또는 no signal 상태의 failure
- `run.failed_source_count=1`
- `run.signal_count=0`
- `run.pool_run_count=0`
- `run.last_error` 또는 `run.message`에 실패 원인 포함

통과 기준:
- preview/mock/fallback track이 생성되지 않음
- `ems_pool_ingest_run.collection_source='acquisition_pool'` 신규 row가 생기지 않음

---

## 시나리오 5 — provider 인증 실패 처리

목적: source/AI 추출은 성공했지만 provider credential이 없거나 만료된 경우 seed 단위 실패가 기록되는지 확인.

1. provider 연결이 없는 test user 또는 만료된 credential user 사용
2. 시나리오 2와 같은 수동 run 실행

검증:
- `run.signal_count > 0`
- `run.seed_count > 0`
- `run.failed_seed_count > 0`
- `seeds[*].status="failed"`
- `seeds[*].last_error`에 `Connect spotify...` 또는 provider credential 원인 표시

통과 기준:
- source/AI 성공과 provider 실패가 분리되어 보임
- 실패한 provider를 다른 provider/mock 데이터로 대체하지 않음

---

## 시나리오 6 — scheduler 실행

목적: 기본 또는 설정된 source로 주기 acquisition이 실행되는지 확인.

1. 기본 source를 쓰거나 env 또는 `application.yml`에 아래 설정:

```yaml
app:
  ems:
    acquisition:
      enabled: true
      user-id: user-001
      initial-delay-ms: 30000
      refresh-interval-ms: 300000
      max-articles-per-source: 10
      max-signals-per-run: 20
      per-seed-limit: 5
      platforms: spotify,tidal
      sources:
        - name: Manual QA RSS
          type: rss
          url: https://example.com/feed.xml
          weight: 1.0
```

2. API 재시작
3. 30초 이후 로그 확인

검증:
- API 로그에 EMS acquisition run 관련 로그 출력
- DB:

```sql
select trigger_type, status, source_count, signal_count, seed_count, pool_run_count, started_at
from ems_acquisition_run
where trigger_type = 'scheduled'
order by started_at desc
limit 5;
```

통과 기준:
- `trigger_type='scheduled'` run 생성
- 동시에 이전 run이 살아 있으면 새 tick은 중복 실행하지 않고 skip 로그만 남김

---

## 시나리오 7 — scale / dedupe / limit 검증

목적: 많은 source와 article을 넣어도 run cap, query variant fan-out, article URL dedupe, seed dedupe가 지켜지는지 확인.

1. source 3개 이상, `max_articles_per_source=20`, `max_signals_per_run=15`, `platforms=["spotify","tidal"]`로 실행
2. 같은 입력으로 한 번 더 실행
3. DB 확인:

```sql
select signal_count, seed_count, skipped_article_count, skipped_seed_count
from ems_acquisition_run
order by started_at desc
limit 1;

select platform_id, lower(query), count(*)
from ems_acquisition_seed
where ems_acquisition_run_id = (
  select ems_acquisition_run_id
  from ems_acquisition_run
  order by started_at desc
  limit 1
)
group by platform_id, lower(query)
having count(*) > 1;
```

통과 기준:
- `signal_count <= max_signals_per_run`
- 중복 seed query가 같은 platform에 두 번 생성되지 않음
- 이미 성공적으로 queue된 같은 platform/query seed는 다음 run에서 다시 POOL에 들어가지 않음
- 이미 처리된 article URL만 들어온 source는 AI signal extraction을 다시 호출하지 않음
- AI는 signal당 primary query + 최대 3개 `query_variants`를 만들 수 있음
- `seed_count <= signal_count * platform_count * 4`

---

## 시나리오 8 — source weight / confidence 검증

목적: source weight가 signal confidence에 반영되고 1.0을 넘지 않는지 확인.

1. 같은 source를 `weight=0.5`와 `weight=2.0`으로 각각 실행
2. DB 확인:

```sql
select source_name, query, confidence_score
from ems_acquisition_signal
order by created_at desc
limit 30;
```

통과 기준:
- 모든 `confidence_score`는 `0.0000` 이상 `1.0000` 이하
- 높은 weight source가 낮은 weight source보다 같은 model confidence에서 더 높은 score를 가질 수 있음

---

## 시나리오 9 — 기존 EMS search pool 회귀 검증

목적: acquisition 추가가 기존 `/api/v1/ems/collection/search`와 `/ems/pool-admin` 흐름을 깨지 않았는지 확인.

1. `/ems`에서 기존 검색 실행 또는 API 직접 호출:

```bash
curl -s http://127.0.0.1:8080/api/v1/ems/collection/search \
  -H 'Content-Type: application/json' \
  -d '{
    "user_id": "user-001",
    "platform_id": "spotify",
    "query": "jazz"
  }'
```

검증:
- 응답 `status="ems_search_pooled"`
- `pool_run_id` 존재
- DB에서 해당 run의 `collection_source='search_pool'`
- `/ems/pool-admin`에서 기존 search pool run이 보이고 처리 가능

---

## 시나리오 10 — 자동화 테스트 회귀

명령:

```bash
cd services/api
./gradlew test
```

```bash
services/ai/.venv/bin/python -m pytest services/ai/tests
```

통과 기준:
- API 전체 테스트 통과
- AI 전체 테스트 통과
- 신규 테스트 포함:
  - `EmsAcquisitionServiceTest`
  - `EmsAcquisitionControllerWebMvcTest`
  - AI `test_ems_acquisition_*`

---

## 최종 통과 기준

- source fetch 실패, AI 실패, provider 실패가 각각 run/source/seed 단위로 드러남
- AI model 미설정 또는 provider credential 문제를 mock/preview 데이터로 숨기지 않음
- acquisition으로 생성된 provider 결과는 반드시 `ems_pool_*`를 거쳐 EMS 본 테이블에 반영됨
- EMS 본 테이블의 acquisition 데이터는 `collection_source='acquisition_pool'`로 식별 가능
- 기존 `search_pool` 흐름은 그대로 동작
