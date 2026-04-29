# services/ai

FastAPI 기반 AI/추천 서비스 폴더입니다.

## 목표

- 추천 계산
- 임베딩/모델 추론
- AI 보강 API
- `services/api`가 호출하는 AI 전용 백엔드 제공

## 현재 스캐폴드에 포함된 것

- `app.main` 실행 엔트리포인트
- 기본 정보 엔드포인트: `/`
- 헬스체크 엔드포인트: `/health`
- 추천 미리보기 엔드포인트: `POST /v1/recommendations/preview`
- FastAPI 문서 경로: `/docs`
- OpenAPI 경로: `/openapi.json`
- 최소 테스트 파일: `tests/test_health.py`

## 권장 구조

```text
services/ai/
├── app/
│   ├── config.py
│   ├── main.py
│   ├── routers/
│   ├── schemas/
│   ├── services/
│   ├── models/
│   └── jobs/
├── tests/
├── requirements.txt
├── requirements-dev.txt
└── README.md
```

## 환경 변수

- `AI_APP_NAME`: 서비스 이름, 기본값 `My Forever Music AI`
- `AI_APP_VERSION`: 서비스 버전, 기본값 `0.1.0`
- `AI_ENV`: 실행 환경, 기본값 `local`
- `AI_ROOT_PATH`: Nginx 뒤에서 `/ai/*`로 공개할 때 사용할 prefix

`AI_ROOT_PATH`는 직접 `8000` 포트로 접근할 때는 비워두고, Ubuntu 서버에서 Nginx가 `/ai/`로 프록시할 때는 `/ai`로 주는 것을 권장합니다.

## 실행 예시

직접 포트로 실행:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements-dev.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Nginx `/ai/` 프록시 뒤에서 실행:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements-dev.txt
AI_ROOT_PATH=/ai uvicorn app.main:app --host 0.0.0.0 --port 8000
```

## 확인 경로

- 직접 실행 시
  - `http://127.0.0.1:8000/`
  - `http://127.0.0.1:8000/health`
  - `http://127.0.0.1:8000/docs`

- Nginx 프록시 뒤 실행 시
  - `http://SERVER_IP/ai/`
  - `http://SERVER_IP/ai/health`
  - `http://SERVER_IP/ai/docs`

## 테스트

```bash
pytest
```

## 추천 API 초안

현재는 `services/api`가 붙기 전 단계라, 실제 모델 추론 대신 계약과 응답 구조를 먼저 고정하는 preview 엔드포인트를 제공합니다.

- 경로: `POST /v1/recommendations/preview`
- 용도: PMS / EMS / GMS 추천 흐름에서 AI 서비스 응답 형태를 먼저 검증
- 특성: 실제 음원 카탈로그 조회 없이 rule-based preview 응답 생성

예시 요청:

```json
{
  "request_id": "preview-001",
  "user_id": "user-123",
  "playlist_id": "playlist-001",
  "mode": "gms",
  "mood": "upbeat",
  "energy_level": 4,
  "familiarity_bias": 3,
  "limit": 5,
  "seed_track_ids": ["track-alpha", "track-beta"],
  "seed_artist_names": ["Artist One"],
  "seed_genres": ["synth-pop"],
  "include_explanations": true
}
```

예시 호출:

```bash
curl -X POST http://127.0.0.1:8000/v1/recommendations/preview \
  -H 'Content-Type: application/json' \
  -d '{
    "mode": "gms",
    "mood": "upbeat",
    "limit": 3,
    "seed_track_ids": ["track-alpha", "track-beta"]
  }'
```

상세 계약 문서는 [AI_RECOMMENDATION_PREVIEW.md](/Users/woosungjo/music-space/my-forever-music/docs/api/AI_RECOMMENDATION_PREVIEW.md) 에 정리했습니다.

## 다음 구현 우선순위

1. `services/api` 호출용 내부 계약과 에러 코드 정리
2. 실제 카탈로그/벡터 검색 기반 ranking 로직 연결
3. 모델 로딩 계층과 비동기 작업 분리
