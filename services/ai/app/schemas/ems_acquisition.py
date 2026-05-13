from __future__ import annotations

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

SignalType = Literal["track", "artist", "playlist_query", "genre", "scene"]


class EmsAcquisitionArticle(BaseModel):
    model_config = ConfigDict(extra="forbid")

    article_url: str | None = None
    title: str
    summary: str | None = None
    published_at: datetime | None = None


class EmsAcquisitionSignalRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    source_name: str
    source_url: str
    source_weight: float = Field(default=1.0, ge=0.0, le=5.0)
    articles: list[EmsAcquisitionArticle] = Field(default_factory=list, max_length=50)
    max_signals: int = Field(default=20, ge=1, le=200)


class EmsAcquisitionSignal(BaseModel):
    model_config = ConfigDict(extra="forbid")

    article_url: str | None = None
    article_title: str | None = None
    signal_type: SignalType
    query: str = Field(min_length=1, max_length=200)
    confidence_score: float = Field(ge=0.0, le=1.0)
    rationale: str = Field(default="", max_length=500)


class EmsAcquisitionSignalResponse(BaseModel):
    request_id: str
    generated_at: datetime
    service: str
    status: Literal["ok"]
    model: str
    signals: list[EmsAcquisitionSignal]
