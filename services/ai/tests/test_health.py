from fastapi.testclient import TestClient
import pytest

from app.config import get_settings
from app.main import app
from app.schemas.ems_overview import (
    EmsDeterministicRecommendation,
    EmsOverviewRequest,
    EmsProviderPool,
    EmsSignal,
)
from app.services.ems_overview_service import EmsOverviewService

client = TestClient(app)


def test_root_returns_service_metadata() -> None:
    response = client.get("/")

    assert response.status_code == 200
    payload = response.json()
    assert payload["service"] == "ai"
    assert payload["status"] == "ok"
    assert any(item["path"] == "/health" for item in payload["endpoints"])


def test_health_returns_ok() -> None:
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_recommendation_preview_returns_requested_limit() -> None:
    response = client.post(
        "/v1/recommendations/preview",
        json={
            "mode": "gms",
            "mood": "upbeat",
            "limit": 3,
            "seed_track_ids": ["track-alpha", "track-beta"],
            "seed_artist_names": ["Artist One"],
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["context"]["strategy"] == "gms-hybrid-blend"
    assert len(payload["items"]) == 3
    assert payload["items"][0]["source_space"] == "gms"


def test_recommendation_preview_uses_fallback_when_seeds_are_missing() -> None:
    response = client.post(
        "/v1/recommendations/preview",
        json={
            "mode": "discovery",
            "limit": 2,
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["context"]["strategy"] == "discovery-fallback"
    assert len(payload["warnings"]) == 1


def test_ems_overview_requires_configured_llm_model() -> None:
    response = client.post(
        "/v1/ems/overview",
        json={
            "user_id": "user-001",
            "playlist_id": "playlist-001",
            "playlist_title": "Morning Library",
            "playlist_count": 2,
            "library_track_count": 120,
            "seed_track_count": 5,
            "artist_seed_count": 3,
            "genre_seed_count": 2,
            "recommendation": {
                "mood": "calm",
                "energy_level": 2,
                "familiarity_bias": 3,
                "confidence_score": 0.72,
            },
            "top_signals": [],
            "provider_pools": [
                {
                    "platform_id": "tidal",
                    "playlist_count": 12,
                    "track_count": 300,
                    "last_collected_at": None,
                }
            ],
            "warnings": [],
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["status"] == "model_not_configured"
    assert payload["readiness_status"] == "llm_model_required"
    assert payload["taste_model_snapshot"] is None


def test_ems_overview_accepts_schema_compliant_llm_response(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("AI_EMS_OVERVIEW_MODEL", "test-model")
    monkeypatch.setenv("AI_LLM_API_KEY", "test-key")
    get_settings.cache_clear()

    monkeypatch.setattr(
        EmsOverviewService,
        "_call_llm",
        lambda self, settings, request, evidence: {
            "taste_model_snapshot": "Warm jazz-pop seeds are driving a balanced discovery profile.",
            "candidate_direction": "Promote nearby vocal jazz and mellow pop candidates into GMS review.",
            "readiness_status": "ready_for_gms_review",
            "attention_items": [],
            "evidence": ["LLM used deterministic PMS and EMS pool evidence only."],
        },
    )

    response = EmsOverviewService().interpret(sample_ems_overview_request())

    assert response.status == "ok"
    assert response.taste_model_snapshot == "Warm jazz-pop seeds are driving a balanced discovery profile."
    assert response.candidate_direction.startswith("Promote nearby")
    get_settings.cache_clear()


def test_ems_overview_rejects_invalid_llm_response_shape(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("AI_EMS_OVERVIEW_MODEL", "test-model")
    monkeypatch.setenv("AI_LLM_API_KEY", "test-key")
    get_settings.cache_clear()

    monkeypatch.setattr(
        EmsOverviewService,
        "_call_llm",
        lambda self, settings, request, evidence: {
            "taste_model_snapshot": {"summary": "wrong shape"},
            "candidate_direction": "Promote nearby candidates.",
            "readiness_status": "ready_for_gms_review",
            "attention_items": [],
            "evidence": [],
        },
    )

    with pytest.raises(Exception) as exception:
        EmsOverviewService().interpret(sample_ems_overview_request())

    assert "expected a non-empty string" in str(exception.value)
    get_settings.cache_clear()


def sample_ems_overview_request() -> EmsOverviewRequest:
    return EmsOverviewRequest(
        user_id="user-001",
        playlist_id="playlist-001",
        playlist_title="Morning Library",
        playlist_count=2,
        library_track_count=120,
        seed_track_count=5,
        artist_seed_count=3,
        genre_seed_count=2,
        recommendation=EmsDeterministicRecommendation(
            mood="calm",
            energy_level=2,
            familiarity_bias=3,
            confidence_score=0.72,
        ),
        top_signals=[
            EmsSignal(
                type="genre",
                label="jazz pop",
                weight=1.2,
                reason="Seeds lean toward warm vocal jazz pop.",
            )
        ],
        provider_pools=[
            EmsProviderPool(
                platform_id="tidal",
                playlist_count=12,
                track_count=300,
                last_collected_at=None,
            )
        ],
        warnings=[],
    )
