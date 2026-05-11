from fastapi.testclient import TestClient

from app.main import app
from app.schemas.recommendation_dataset import RecommendationDatasetImportRequest
from app.services.recommendation_dataset_service import RecommendationDatasetService
from app.services.sasrec_dataset_service import SasrecDatasetService
from app.services.sasrec_offline_report_service import SasrecOfflineReportService
from app.services.sasrec_training_service import SasrecTrainingService

client = TestClient(app)


def test_validate_recommendation_dataset_returns_training_readiness() -> None:
    response = client.post(
        "/v1/recommendations/datasets/validate",
        json=sample_dataset_payload(),
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["service"] == "recommendation-dataset"
    assert payload["readiness_status"] == "ready_for_training"
    assert payload["sequence_item_count"] == 2
    assert payload["event_count"] == 1
    assert payload["recommendation_snapshot_count"] == 1
    assert payload["unique_track_count"] == 2
    assert payload["positive_signal_count"] == 1
    assert payload["negative_signal_count"] == 0
    assert payload["token_preview"][0] == "event:track_saved:track-001"


def test_validate_recommendation_dataset_reports_empty_sequence() -> None:
    payload = sample_dataset_payload()
    payload["summary"]["sequence_item_count"] = 0
    payload["sequence"] = []

    response = client.post("/v1/recommendations/datasets/validate", json=payload)

    assert response.status_code == 200
    assert response.json()["readiness_status"] == "empty_sequence"


def test_dataset_service_reports_mismatched_summary_counts() -> None:
    payload = sample_dataset_payload()
    payload["summary"]["event_count"] = 99

    result = RecommendationDatasetService().validate_import(
        RecommendationDatasetImportRequest.model_validate(payload)
    )

    assert result.readiness_status == "needs_more_events"
    assert result.warnings == [
        "summary.event_count does not match the number of event payload items."
    ]


def test_prepare_sasrec_dataset_returns_vocabulary_and_training_windows() -> None:
    response = client.post(
        "/v1/recommendations/datasets/sasrec/prepare",
        params={"max_context_length": 3},
        json=sample_dataset_payload(),
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["service"] == "sasrec-dataset"
    assert payload["summary"]["unique_track_count"] == 2
    assert payload["summary"]["training_example_count"] == 1
    assert payload["vocabulary"] == [
        {"track_id": "track-001", "item_index": 1},
        {"track_id": "track-002", "item_index": 2},
    ]
    assert payload["training_examples"][0]["context_item_indices"] == [1]
    assert payload["training_examples"][0]["target_item_index"] == 2
    assert payload["training_examples"][0]["weight"] == 0.88


def test_sasrec_dataset_service_weights_positive_events() -> None:
    payload = sample_dataset_payload()
    payload["sequence"] = [
        {
            "item_type": "recommendation_snapshot",
            "source_id": 1,
            "token": "recommendation:gms-baseline-v1:track-002",
            "track_id": "track-002",
            "playlist_id": "playlist-002",
            "recommendation_id": "recommendation-001",
            "weight": 0.88,
            "occurred_at": "2026-05-11T01:00:00Z",
        },
        {
            "item_type": "event",
            "source_id": 1,
            "token": "event:track_saved:track-001",
            "track_id": "track-001",
            "playlist_id": "playlist-001",
            "recommendation_id": "recommendation-001",
            "weight": 0.1,
            "occurred_at": "2026-05-11T02:00:00Z",
        },
    ]

    result = SasrecDatasetService().prepare_dataset(
        RecommendationDatasetImportRequest.model_validate(payload)
    )

    assert result.training_examples[0].target_track_id == "track-001"
    assert result.training_examples[0].weight == 1.0


def test_sasrec_offline_report_returns_leave_last_out_metrics() -> None:
    payload = sample_dataset_payload()
    payload["summary"]["sequence_item_count"] = 3
    payload["sequence"].append(
        {
            "item_type": "event",
            "source_id": 1,
            "token": "event:track_saved:track-001",
            "track_id": "track-001",
            "playlist_id": "playlist-001",
            "recommendation_id": "recommendation-001",
            "weight": 1.0,
            "occurred_at": "2026-05-11T03:00:00Z",
        }
    )

    response = client.post(
        "/v1/recommendations/datasets/sasrec/offline-report",
        params={"k": 2},
        json=payload,
    )

    assert response.status_code == 200
    result = response.json()
    assert result["service"] == "sasrec-offline-report"
    assert result["train_example_count"] == 1
    assert result["evaluation_example_count"] == 1
    assert result["metrics"]["hit_rate_at_k"] == 1.0
    assert result["metrics"]["mrr_at_k"] == 0.5
    assert result["metrics"]["ndcg_at_k"] == 0.6309
    assert result["evaluation_examples"][0]["hit_rank"] == 2


def test_sasrec_offline_report_service_handles_empty_dataset() -> None:
    payload = sample_dataset_payload()
    payload["summary"] = {
        "event_count": 0,
        "recommendation_snapshot_count": 0,
        "sequence_item_count": 0,
    }
    payload["events"] = []
    payload["recommendation_snapshots"] = []
    payload["sequence"] = []

    result = SasrecOfflineReportService().build_report(
        RecommendationDatasetImportRequest.model_validate(payload)
    )

    assert result.metrics.hit_rate_at_k == 0.0
    assert result.evaluation_example_count == 0
    assert "Offline report has no evaluation example." in result.warnings


def test_train_sasrec_mvp_runs_pytorch_training_loop() -> None:
    payload = sasrec_training_payload()

    response = client.post(
        "/v1/recommendations/datasets/sasrec/train",
        params={
            "max_context_length": 4,
            "epochs": 3,
            "hidden_size": 8,
            "k": 3,
        },
        json=payload,
    )

    assert response.status_code == 200
    result = response.json()
    assert result["service"] == "sasrec-mvp-training"
    assert result["model_version"].startswith("sasrec-mvp-")
    assert result["summary"]["vocabulary_size"] == 3
    assert result["summary"]["train_example_count"] == 2
    assert result["summary"]["evaluation_example_count"] == 1
    assert result["summary"]["epoch_count"] == 3
    assert result["summary"]["final_loss"] is not None
    assert len(result["evaluation_examples"][0]["predicted_item_indices"]) == 3


def test_sasrec_training_service_skips_when_train_split_is_empty() -> None:
    result = SasrecTrainingService().train_mvp(
        RecommendationDatasetImportRequest.model_validate(sample_dataset_payload())
    )

    assert result.summary.train_example_count == 0
    assert result.summary.epoch_count == 0
    assert result.summary.final_loss is None
    assert "SASRec MVP training skipped because there is no train split before evaluation." in result.warnings


def sample_dataset_payload() -> dict:
    return {
        "user_id": "user-001",
        "generated_at": "2026-05-11T03:00:00Z",
        "event_limit": 300,
        "snapshot_limit": 200,
        "summary": {
            "event_count": 1,
            "recommendation_snapshot_count": 1,
            "sequence_item_count": 2,
        },
        "events": [
            {
                "event_id": 1,
                "event_type": "track_saved",
                "event_weight": 1.0,
                "source_space": "gms",
                "source_platform": "spotify",
                "track_id": "track-001",
                "playlist_id": "playlist-001",
                "item_id": "track-001",
                "item_kind": "track",
                "title": "Midnight Receiver",
                "artist_name": "Neon Bloom",
                "recommendation_id": "recommendation-001",
                "metadata_confidence": 0.95,
                "occurred_at": "2026-05-11T01:00:00Z",
            }
        ],
        "recommendation_snapshots": [
            {
                "snapshot_id": 1,
                "recommendation_id": "recommendation-001",
                "request_id": "request-001",
                "candidate_track_id": "track-002",
                "candidate_playlist_id": "playlist-002",
                "candidate_title": "Signal Run",
                "candidate_artist_name": "Neon Bloom",
                "source_space": "gms",
                "source_platform": "tidal",
                "model_version": "gms-baseline-v1",
                "feature_snapshot_id": "audio-track-002",
                "affinity_score": 0.88,
                "novelty_score": 0.75,
                "coherence_score": 0.82,
                "diversity_score": 0.93,
                "redundancy_penalty": 0.0,
                "confidence_score": 0.95,
                "rank": 1,
                "created_at": "2026-05-11T02:00:00Z",
            }
        ],
        "sequence": [
            {
                "item_type": "event",
                "source_id": 1,
                "token": "event:track_saved:track-001",
                "track_id": "track-001",
                "playlist_id": "playlist-001",
                "recommendation_id": "recommendation-001",
                "weight": 1.0,
                "occurred_at": "2026-05-11T01:00:00Z",
            },
            {
                "item_type": "recommendation_snapshot",
                "source_id": 1,
                "token": "recommendation:gms-baseline-v1:track-002",
                "track_id": "track-002",
                "playlist_id": "playlist-002",
                "recommendation_id": "recommendation-001",
                "weight": 0.88,
                "occurred_at": "2026-05-11T02:00:00Z",
            },
        ],
    }


def sasrec_training_payload() -> dict:
    payload = sample_dataset_payload()
    payload["summary"] = {
        "event_count": 1,
        "recommendation_snapshot_count": 1,
        "sequence_item_count": 4,
    }
    payload["sequence"] = [
        {
            "item_type": "event",
            "source_id": 1,
            "token": "event:track_saved:track-001",
            "track_id": "track-001",
            "playlist_id": "playlist-001",
            "recommendation_id": "recommendation-001",
            "weight": 1.0,
            "occurred_at": "2026-05-11T01:00:00Z",
        },
        {
            "item_type": "recommendation_snapshot",
            "source_id": 1,
            "token": "recommendation:gms-baseline-v1:track-002",
            "track_id": "track-002",
            "playlist_id": "playlist-002",
            "recommendation_id": "recommendation-001",
            "weight": 0.88,
            "occurred_at": "2026-05-11T02:00:00Z",
        },
        {
            "item_type": "recommendation_snapshot",
            "source_id": 1,
            "token": "recommendation:gms-baseline-v1:track-003",
            "track_id": "track-003",
            "playlist_id": "playlist-002",
            "recommendation_id": "recommendation-001",
            "weight": 0.76,
            "occurred_at": "2026-05-11T03:00:00Z",
        },
        {
            "item_type": "event",
            "source_id": 1,
            "token": "event:track_saved:track-002",
            "track_id": "track-002",
            "playlist_id": "playlist-001",
            "recommendation_id": "recommendation-001",
            "weight": 1.0,
            "occurred_at": "2026-05-11T04:00:00Z",
        },
    ]
    return payload
