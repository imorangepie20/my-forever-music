from __future__ import annotations

from datetime import datetime, timezone
import json
from urllib import request as urllib_request
from urllib.error import HTTPError, URLError
from uuid import uuid4

from fastapi import HTTPException

from app.config import get_settings
from app.schemas.ems_overview import EmsOverviewRequest, EmsOverviewResponse


class EmsOverviewService:
    def interpret(self, request: EmsOverviewRequest) -> EmsOverviewResponse:
        settings = get_settings()
        request_id = f"ai-ems-overview-{uuid4().hex[:12]}"
        evidence = self._build_evidence(request)
        attention_items = list(request.warnings)

        if not settings.ems_overview_model or not settings.llm_api_key:
            attention_items.append(
                "EMS overview LLM is not configured. Set AI_EMS_OVERVIEW_MODEL and AI_LLM_API_KEY to enable the interpretation layer."
            )
            return EmsOverviewResponse(
                request_id=request_id,
                generated_at=datetime.now(timezone.utc),
                service="ai",
                status="model_not_configured",
                model=settings.ems_overview_model or None,
                readiness_status="llm_model_required",
                attention_items=attention_items,
                evidence=evidence,
                confidence=request.recommendation.confidence_score,
            )

        interpreted = self._call_llm(settings, request, evidence)
        return EmsOverviewResponse(
            request_id=request_id,
            generated_at=datetime.now(timezone.utc),
            service="ai",
            status="ok",
            model=settings.ems_overview_model,
            taste_model_snapshot=self._required_text(interpreted, "taste_model_snapshot"),
            candidate_direction=self._required_text(interpreted, "candidate_direction"),
            readiness_status=self._readiness_status(interpreted),
            attention_items=self._merge_unique(attention_items, self._text_list(interpreted, "attention_items")),
            evidence=self._merge_unique(evidence, self._text_list(interpreted, "evidence")),
            confidence=request.recommendation.confidence_score,
        )

    def _call_llm(self, settings, request: EmsOverviewRequest, evidence: list[str]) -> dict:
        payload = {
            "model": settings.ems_overview_model,
            "temperature": 0.2,
            "response_format": {
                "type": "json_schema",
                "json_schema": {
                    "name": "ems_overview_interpretation",
                    "strict": True,
                    "schema": {
                        "type": "object",
                        "additionalProperties": False,
                        "required": [
                            "taste_model_snapshot",
                            "candidate_direction",
                            "readiness_status",
                            "attention_items",
                            "evidence",
                        ],
                        "properties": {
                            "taste_model_snapshot": {"type": "string"},
                            "candidate_direction": {"type": "string"},
                            "readiness_status": {
                                "type": "string",
                                "enum": [
                                    "ready_for_gms_review",
                                    "partial_signal_coverage",
                                    "needs_pms_library",
                                    "waiting_for_ems_pool",
                                ],
                            },
                            "attention_items": {
                                "type": "array",
                                "items": {"type": "string"},
                            },
                            "evidence": {
                                "type": "array",
                                "items": {"type": "string"},
                            },
                        },
                    },
                },
            },
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You summarize a music recommendation engine state. "
                        "Do not invent songs, providers, playlists, or model facts. "
                        "Use only the JSON input. Return compact JSON matching the response schema exactly. "
                        "taste_model_snapshot, candidate_direction, and readiness_status must be strings, not objects. "
                        "readiness_status must be one of: ready_for_gms_review, partial_signal_coverage, "
                        "needs_pms_library, waiting_for_ems_pool. Put explanations in candidate_direction or attention_items, "
                        "never in readiness_status. "
                        "attention_items and evidence must be arrays of strings. Return these keys only: "
                        "taste_model_snapshot, candidate_direction, readiness_status, "
                        "attention_items, evidence."
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "overview_context": request.model_dump(),
                            "deterministic_evidence": evidence,
                        },
                        ensure_ascii=False,
                    ),
                },
            ],
        }

        endpoint = settings.llm_base_url.rstrip("/") + "/chat/completions"
        http_request = urllib_request.Request(
            endpoint,
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Authorization": f"Bearer {settings.llm_api_key}",
                "Content-Type": "application/json",
                "Accept": "application/json",
            },
            method="POST",
        )

        try:
            with urllib_request.urlopen(http_request, timeout=20) as response:
                body = json.loads(response.read().decode("utf-8"))
        except HTTPError as exception:
            raise HTTPException(
                status_code=502,
                detail=f"EMS overview LLM request failed with status {exception.code}.",
            ) from exception
        except (URLError, TimeoutError) as exception:
            raise HTTPException(
                status_code=502,
                detail="EMS overview LLM endpoint is unreachable.",
            ) from exception

        content = (
            body.get("choices", [{}])[0]
            .get("message", {})
            .get("content")
        )
        if not content:
            raise HTTPException(status_code=502, detail="EMS overview LLM returned an empty message.")

        try:
            parsed = json.loads(content)
        except json.JSONDecodeError as exception:
            raise HTTPException(status_code=502, detail="EMS overview LLM returned invalid JSON.") from exception

        if not isinstance(parsed, dict):
            raise HTTPException(status_code=502, detail="EMS overview LLM returned an invalid JSON shape.")
        return parsed

    def _readiness_status(self, parsed: dict) -> str:
        value = self._required_text(parsed, "readiness_status")
        allowed_statuses = {
            "ready_for_gms_review",
            "partial_signal_coverage",
            "needs_pms_library",
            "waiting_for_ems_pool",
        }
        if value not in allowed_statuses:
            raise HTTPException(
                status_code=502,
                detail="EMS overview LLM returned invalid `readiness_status`; expected a known status code.",
            )
        return value

    def _required_text(self, parsed: dict, key: str) -> str:
        value = parsed.get(key)
        if not isinstance(value, str) or not value.strip():
            raise HTTPException(
                status_code=502,
                detail=f"EMS overview LLM returned invalid `{key}`; expected a non-empty string.",
            )
        return value

    def _text_list(self, parsed: dict, key: str) -> list[str]:
        value = parsed.get(key)
        if value is None:
            return []
        if not isinstance(value, list) or not all(isinstance(item, str) for item in value):
            raise HTTPException(
                status_code=502,
                detail=f"EMS overview LLM returned invalid `{key}`; expected an array of strings.",
            )
        return value

    def _merge_unique(self, first: list[str], second: list[str]) -> list[str]:
        merged = []
        seen = set()
        for item in first + second:
            if item in seen:
                continue
            seen.add(item)
            merged.append(item)
        return merged

    def _build_evidence(self, request: EmsOverviewRequest) -> list[str]:
        evidence = [
            f"PMS playlists={request.playlist_count}, library_tracks={request.library_track_count}",
            (
                "Seeds: tracks=%d, artists=%d, genres=%d"
                % (request.seed_track_count, request.artist_seed_count, request.genre_seed_count)
            ),
        ]
        if request.recommendation.mood:
            evidence.append(
                "Deterministic EMS recommendation: mood=%s, energy=%s, familiarity=%s"
                % (
                    request.recommendation.mood,
                    request.recommendation.energy_level,
                    request.recommendation.familiarity_bias,
                )
            )
        for provider in request.provider_pools:
            evidence.append(
                "%s EMS pool: playlists=%d, tracks=%d, audio_features=%d/%d"
                % (
                    provider.platform_id,
                    provider.playlist_count,
                    provider.track_count,
                    provider.audio_feature_filled_track_count,
                    provider.track_count,
                )
            )
        return evidence
