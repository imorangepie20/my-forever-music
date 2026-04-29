from fastapi.testclient import TestClient

from app.main import app

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
