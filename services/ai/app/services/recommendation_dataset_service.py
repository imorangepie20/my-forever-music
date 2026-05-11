from __future__ import annotations

from uuid import uuid5, NAMESPACE_URL

from app.schemas.recommendation_dataset import (
    DatasetReadinessStatus,
    RecommendationDatasetImportRequest,
    RecommendationDatasetValidationResponse,
)

POSITIVE_EVENT_TYPES = {
    "play_completed",
    "repeat_played",
    "replay",
    "track_saved",
    "added_to_playlist",
    "recommendation_liked",
}

NEGATIVE_EVENT_TYPES = {
    "skipped_early",
    "skip_next",
    "recommendation_rejected",
    "ignored_recommendation",
    "stopped_midway",
}


class RecommendationDatasetService:
    def validate_import(
        self,
        request: RecommendationDatasetImportRequest,
    ) -> RecommendationDatasetValidationResponse:
        warnings = self._validate_counts(request)
        unique_track_ids = self._unique_track_ids(request)
        positive_signal_count = self._count_events(request, POSITIVE_EVENT_TYPES)
        negative_signal_count = self._count_events(request, NEGATIVE_EVENT_TYPES)
        readiness_status = self._resolve_readiness(request, warnings)

        return RecommendationDatasetValidationResponse(
            dataset_id=self._dataset_id(request),
            service="recommendation-dataset",
            status="ok",
            user_id=request.user_id,
            readiness_status=readiness_status,
            sequence_item_count=len(request.sequence),
            event_count=len(request.events),
            recommendation_snapshot_count=len(request.recommendation_snapshots),
            unique_track_count=len(unique_track_ids),
            positive_signal_count=positive_signal_count,
            negative_signal_count=negative_signal_count,
            token_preview=[item.token for item in request.sequence[:10]],
            warnings=warnings,
        )

    def _validate_counts(self, request: RecommendationDatasetImportRequest) -> list[str]:
        warnings: list[str] = []
        if request.summary.event_count != len(request.events):
            warnings.append(
                "summary.event_count does not match the number of event payload items."
            )
        if request.summary.recommendation_snapshot_count != len(request.recommendation_snapshots):
            warnings.append(
                "summary.recommendation_snapshot_count does not match the number of recommendation snapshots."
            )
        if request.summary.sequence_item_count != len(request.sequence):
            warnings.append(
                "summary.sequence_item_count does not match the number of sequence items."
            )

        event_ids = {event.event_id for event in request.events}
        snapshot_ids = {snapshot.snapshot_id for snapshot in request.recommendation_snapshots}
        missing_sources = [
            item.token
            for item in request.sequence
            if (item.item_type == "event" and item.source_id not in event_ids)
            or (
                item.item_type == "recommendation_snapshot"
                and item.source_id not in snapshot_ids
            )
        ]
        if missing_sources:
            warnings.append(
                "sequence contains items whose source_id is missing from events or recommendation_snapshots."
            )

        if request.sequence != sorted(request.sequence, key=lambda item: item.occurred_at):
            warnings.append("sequence is not sorted by occurred_at.")

        return warnings

    def _unique_track_ids(self, request: RecommendationDatasetImportRequest) -> set[str]:
        track_ids = {
            item.track_id.strip()
            for item in request.sequence
            if item.track_id is not None and item.track_id.strip()
        }
        track_ids.update(
            event.track_id.strip()
            for event in request.events
            if event.track_id is not None and event.track_id.strip()
        )
        track_ids.update(
            snapshot.candidate_track_id.strip()
            for snapshot in request.recommendation_snapshots
            if snapshot.candidate_track_id is not None and snapshot.candidate_track_id.strip()
        )
        return track_ids

    def _count_events(
        self,
        request: RecommendationDatasetImportRequest,
        event_types: set[str],
    ) -> int:
        return sum(1 for event in request.events if event.event_type in event_types)

    def _resolve_readiness(
        self,
        request: RecommendationDatasetImportRequest,
        warnings: list[str],
    ) -> DatasetReadinessStatus:
        if not request.sequence:
            return "empty_sequence"
        if not request.events:
            return "needs_more_events"
        if not request.recommendation_snapshots:
            return "needs_recommendation_snapshots"
        if warnings:
            return "needs_more_events"
        return "ready_for_training"

    def _dataset_id(self, request: RecommendationDatasetImportRequest) -> str:
        seed = f"{request.user_id}:{request.generated_at.isoformat()}:{len(request.sequence)}"
        return f"rec-dataset-{uuid5(NAMESPACE_URL, seed).hex[:16]}"
