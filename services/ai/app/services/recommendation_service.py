from __future__ import annotations

from datetime import datetime, timezone
import re
from uuid import uuid4

from app.schemas.recommendation import (
    RecommendationContext,
    RecommendationInputSummary,
    RecommendationItem,
    RecommendationPreviewRequest,
    RecommendationPreviewResponse,
    RecommendationStrategy,
)

TITLE_SUFFIXES = [
    "Echo",
    "Drift",
    "Pulse",
    "Bloom",
    "Night Mix",
]

ARTIST_PREFIXES = [
    "The",
    "Nova",
    "Blue",
    "Silver",
    "Velvet",
]

MOOD_DEFAULT_ENERGY = {
    "focus": 3,
    "calm": 2,
    "upbeat": 4,
    "melancholy": 2,
    "discovery": 3,
}


class RecommendationService:
    def generate_preview(
        self,
        request: RecommendationPreviewRequest,
    ) -> RecommendationPreviewResponse:
        seed_basis = self._build_seed_basis(request)
        warnings: list[str] = []
        if not seed_basis:
            seed_basis = self._fallback_seed_basis(request)
            warnings.append(
                "No explicit seeds were provided, so the response was generated from a discovery fallback."
            )

        strategy = self._resolve_strategy(request, seed_basis, warnings)
        energy_level = request.energy_level or MOOD_DEFAULT_ENERGY.get(request.mood or "discovery", 3)

        items = [
            self._build_item(
                rank=index + 1,
                seed_token=seed_basis[index % len(seed_basis)],
                request=request,
                strategy=strategy,
                energy_level=energy_level,
            )
            for index in range(request.limit)
        ]

        return RecommendationPreviewResponse(
            request_id=request.request_id or f"ai-rec-{uuid4().hex[:12]}",
            generated_at=datetime.now(timezone.utc),
            service="ai",
            status="ok",
            context=RecommendationContext(
                strategy=strategy,
                engine="rule-based-preview-v1",
                mode=request.mode,
                mood=request.mood,
                energy_level=energy_level,
                seed_basis=seed_basis,
            ),
            input_summary=RecommendationInputSummary(
                user_id=request.user_id,
                playlist_id=request.playlist_id,
                track_seed_count=len(request.seed_track_ids),
                artist_seed_count=len(request.seed_artist_names),
                genre_seed_count=len(request.seed_genres),
                familiarity_bias=request.familiarity_bias,
                limit=request.limit,
            ),
            items=items,
            warnings=warnings,
        )

    def _resolve_strategy(
        self,
        request: RecommendationPreviewRequest,
        seed_basis: list[str],
        warnings: list[str],
    ) -> RecommendationStrategy:
        has_explicit_seeds = bool(
            request.seed_track_ids or request.seed_artist_names or request.seed_genres
        )

        if request.mode == "pms":
            if not has_explicit_seeds:
                warnings.append(
                    "PMS mode is running without playlist or track seeds, so discovery fallback was used."
                )
                return "discovery-fallback"
            return "pms-seed-match"

        if request.mode == "ems":
            if not request.mood:
                warnings.append(
                    "EMS mode is running without a mood target, so discovery fallback was used."
                )
                return "discovery-fallback"
            return "ems-mood-match"

        if request.mode == "discovery" and not has_explicit_seeds:
            return "discovery-fallback"

        if request.mode == "gms":
            if request.mood and has_explicit_seeds:
                return "gms-hybrid-blend"
            if not has_explicit_seeds and seed_basis:
                warnings.append(
                    "GMS mode is running without explicit seeds, so mood and fallback discovery were blended."
                )
                return "discovery-fallback"
            return "gms-hybrid-blend"

        return "discovery-fallback"

    def _build_seed_basis(self, request: RecommendationPreviewRequest) -> list[str]:
        raw_tokens = [
            *request.seed_track_ids,
            *request.seed_artist_names,
            *request.seed_genres,
        ]

        seen: set[str] = set()
        normalized: list[str] = []
        for token in raw_tokens:
            value = self._normalize_token(token)
            if value and value not in seen:
                normalized.append(value)
                seen.add(value)
        return normalized

    def _fallback_seed_basis(self, request: RecommendationPreviewRequest) -> list[str]:
        seeds = [request.mood or request.mode, "library", "discovery"]
        return [self._normalize_token(item) for item in seeds]

    def _build_item(
        self,
        rank: int,
        seed_token: str,
        request: RecommendationPreviewRequest,
        strategy: RecommendationStrategy,
        energy_level: int,
    ) -> RecommendationItem:
        title_root = seed_token.replace("-", " ").title()
        suffix = TITLE_SUFFIXES[(rank - 1) % len(TITLE_SUFFIXES)]
        artist_prefix = ARTIST_PREFIXES[(rank - 1) % len(ARTIST_PREFIXES)]
        score = round(max(0.58, 0.97 - ((rank - 1) * 0.03)), 2)

        reason = None
        if request.include_explanations:
            reason = self._build_reason(
                title_root=title_root,
                request=request,
                strategy=strategy,
                rank=rank,
            )

        return RecommendationItem(
            rank=rank,
            track_id=f"rec-{seed_token}-{rank:02d}",
            title=f"{title_root} {suffix}",
            artist_name=f"{artist_prefix} {title_root}",
            score=score,
            source_space=request.mode,
            energy_level=energy_level,
            reason=reason,
        )

    def _build_reason(
        self,
        title_root: str,
        request: RecommendationPreviewRequest,
        strategy: RecommendationStrategy,
        rank: int,
    ) -> str:
        base = f"{title_root} was selected by {strategy}"

        if request.mood:
            article = "an" if request.mood[0] in {"a", "e", "i", "o", "u"} else "a"
            base = f"{base} to support {article} {request.mood} listening flow"

        if request.seed_track_ids:
            return f"{base} and stays close to the supplied track seeds at rank {rank}."

        if request.seed_artist_names:
            return f"{base} while leaning on the supplied artist affinity at rank {rank}."

        if request.seed_genres:
            return f"{base} while preserving the supplied genre direction at rank {rank}."

        return f"{base} using fallback discovery signals at rank {rank}."

    def _normalize_token(self, value: str) -> str:
        normalized = re.sub(r"[^a-z0-9]+", "-", value.strip().lower())
        return normalized.strip("-")
