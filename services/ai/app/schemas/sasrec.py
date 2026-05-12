from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field


class SasrecVocabularyItem(BaseModel):
    model_config = ConfigDict(extra="forbid")

    track_id: str
    item_index: int = Field(ge=1)


class SasrecTrainingExample(BaseModel):
    model_config = ConfigDict(extra="forbid")

    context_item_indices: list[int]
    target_item_index: int = Field(ge=1)
    target_track_id: str
    source_token: str
    weight: float


class SasrecDatasetSummary(BaseModel):
    model_config = ConfigDict(extra="forbid")

    sequence_item_count: int
    usable_item_count: int
    unique_track_count: int
    training_example_count: int
    max_context_length: int


class SasrecDatasetResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    service: str
    status: str
    user_id: str
    summary: SasrecDatasetSummary
    vocabulary: list[SasrecVocabularyItem]
    training_examples: list[SasrecTrainingExample]
    warnings: list[str]


class SasrecOfflineMetrics(BaseModel):
    model_config = ConfigDict(extra="forbid")

    hit_rate_at_k: float
    mrr_at_k: float
    ndcg_at_k: float


class SasrecEvaluationExample(BaseModel):
    model_config = ConfigDict(extra="forbid")

    target_track_id: str
    target_item_index: int
    predicted_item_indices: list[int]
    hit_rank: int | None = None


class SasrecOfflineReportResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    service: str
    status: str
    user_id: str
    k: int
    train_example_count: int
    evaluation_example_count: int
    metrics: SasrecOfflineMetrics
    evaluation_examples: list[SasrecEvaluationExample]
    warnings: list[str]


class SasrecTrainingSummary(BaseModel):
    model_config = ConfigDict(extra="forbid")

    vocabulary_size: int
    train_example_count: int
    evaluation_example_count: int
    max_context_length: int
    hidden_size: int
    epoch_count: int
    final_loss: float | None = None


class SasrecMetricDelta(BaseModel):
    model_config = ConfigDict(extra="forbid")

    hit_rate_at_k: float
    mrr_at_k: float
    ndcg_at_k: float


class SasrecModelArtifact(BaseModel):
    model_config = ConfigDict(extra="forbid")

    artifact_dir: str | None = None
    model_path: str | None = None
    metadata_path: str | None = None
    saved: bool


class SasrecTrainingResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    service: str
    status: str
    user_id: str
    model_version: str
    summary: SasrecTrainingSummary
    metrics: SasrecOfflineMetrics
    baseline_metrics: SasrecOfflineMetrics
    metric_delta: SasrecMetricDelta
    model_artifact: SasrecModelArtifact
    evaluation_examples: list[SasrecEvaluationExample]
    warnings: list[str]


class SasrecRankingRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    model_version: str = Field(min_length=1)
    context_track_ids: list[str] = Field(default_factory=list)
    candidate_track_ids: list[str] = Field(default_factory=list)
    k: int = Field(default=10, ge=1, le=100)


class SasrecRankedCandidate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    rank: int
    track_id: str
    item_index: int
    score: float


class SasrecRankingResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    service: str
    status: str
    model_version: str
    ranked_candidates: list[SasrecRankedCandidate]
    warnings: list[str]


class SasrecModelRegistryResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    service: str
    status: str
    user_id: str | None = None
    model_version: str | None = None
    artifact_dir: str | None = None
    generated_at: str | None = None
    vocabulary_size: int | None = None
    train_example_count: int | None = None
    warnings: list[str]
