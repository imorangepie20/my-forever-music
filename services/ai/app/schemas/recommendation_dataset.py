from __future__ import annotations

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

DatasetItemType = Literal["event", "recommendation_snapshot"]
DatasetReadinessStatus = Literal[
    "ready_for_training",
    "needs_more_events",
    "needs_recommendation_snapshots",
    "empty_sequence",
]


class RecommendationDatasetSummary(BaseModel):
    model_config = ConfigDict(extra="forbid")

    event_count: int = Field(ge=0)
    recommendation_snapshot_count: int = Field(ge=0)
    sequence_item_count: int = Field(ge=0)
    dataset_version: str | None = None
    dataset_fingerprint: str | None = None


class RecommendationDatasetEvent(BaseModel):
    model_config = ConfigDict(extra="forbid")

    event_id: int
    event_type: str
    event_weight: float | None = None
    source_space: str | None = None
    source_platform: str | None = None
    track_id: str | None = None
    playlist_id: str | None = None
    item_id: str | None = None
    item_kind: str | None = None
    title: str | None = None
    artist_name: str | None = None
    recommendation_id: str | None = None
    metadata_confidence: float | None = None
    occurred_at: datetime


class RecommendationDatasetSnapshot(BaseModel):
    model_config = ConfigDict(extra="forbid")

    snapshot_id: int
    recommendation_id: str
    request_id: str | None = None
    candidate_track_id: str | None = None
    candidate_playlist_id: str | None = None
    candidate_title: str | None = None
    candidate_artist_name: str | None = None
    source_space: str | None = None
    source_platform: str | None = None
    model_version: str
    feature_snapshot_id: str | None = None
    affinity_score: float | None = None
    novelty_score: float | None = None
    coherence_score: float | None = None
    diversity_score: float | None = None
    redundancy_penalty: float | None = None
    confidence_score: float | None = None
    rank: int | None = None
    created_at: datetime


class RecommendationDatasetSequenceItem(BaseModel):
    model_config = ConfigDict(extra="forbid")

    item_type: DatasetItemType
    source_id: int
    token: str
    track_id: str | None = None
    playlist_id: str | None = None
    recommendation_id: str | None = None
    weight: float | None = None
    occurred_at: datetime


class RecommendationDatasetImportRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    user_id: str = Field(min_length=1)
    generated_at: datetime
    event_limit: int = Field(ge=1)
    snapshot_limit: int = Field(ge=1)
    summary: RecommendationDatasetSummary
    events: list[RecommendationDatasetEvent] = Field(default_factory=list)
    recommendation_snapshots: list[RecommendationDatasetSnapshot] = Field(default_factory=list)
    sequence: list[RecommendationDatasetSequenceItem] = Field(default_factory=list)


class RecommendationDatasetValidationResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    dataset_id: str
    service: str
    status: str
    user_id: str
    readiness_status: DatasetReadinessStatus
    sequence_item_count: int
    event_count: int
    recommendation_snapshot_count: int
    unique_track_count: int
    positive_signal_count: int
    negative_signal_count: int
    token_preview: list[str]
    warnings: list[str]
