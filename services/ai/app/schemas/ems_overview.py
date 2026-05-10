from __future__ import annotations

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

OverviewStatus = Literal["ok", "model_not_configured"]


class EmsProviderPool(BaseModel):
    model_config = ConfigDict(extra="forbid")

    platform_id: str
    playlist_count: int = Field(ge=0)
    track_count: int = Field(ge=0)
    audio_feature_filled_track_count: int = Field(default=0, ge=0)
    audio_feature_coverage_ratio: float = Field(default=0.0, ge=0.0, le=1.0)
    last_collected_at: str | None = None


class EmsDeterministicRecommendation(BaseModel):
    model_config = ConfigDict(extra="forbid")

    mood: str | None = None
    energy_level: int | None = None
    familiarity_bias: int | None = None
    confidence_score: float | None = None


class EmsSignal(BaseModel):
    model_config = ConfigDict(extra="forbid")

    type: str
    label: str
    weight: float
    reason: str


class EmsOverviewRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    user_id: str | None = None
    playlist_id: str | None = None
    playlist_title: str | None = None
    playlist_count: int = Field(default=0, ge=0)
    library_track_count: int = Field(default=0, ge=0)
    seed_track_count: int = Field(default=0, ge=0)
    artist_seed_count: int = Field(default=0, ge=0)
    genre_seed_count: int = Field(default=0, ge=0)
    recommendation: EmsDeterministicRecommendation
    top_signals: list[EmsSignal] = Field(default_factory=list)
    provider_pools: list[EmsProviderPool] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)


class EmsOverviewResponse(BaseModel):
    request_id: str
    generated_at: datetime
    service: str
    status: OverviewStatus
    model: str | None = None
    taste_model_snapshot: str | None = None
    candidate_direction: str | None = None
    readiness_status: str
    attention_items: list[str]
    evidence: list[str]
    confidence: float | None = None
