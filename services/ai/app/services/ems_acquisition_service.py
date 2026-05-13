from __future__ import annotations

from datetime import datetime, timezone
import json
from urllib import request as urllib_request
from urllib.error import HTTPError, URLError
from uuid import uuid4

from fastapi import HTTPException

from app.config import get_settings
from app.schemas.ems_acquisition import (
    EmsAcquisitionSignal,
    EmsAcquisitionSignalRequest,
    EmsAcquisitionSignalResponse,
)


class EmsAcquisitionService:
    def extract_signals(self, request: EmsAcquisitionSignalRequest) -> EmsAcquisitionSignalResponse:
        settings = get_settings()
        request_id = f"ai-ems-acquisition-{uuid4().hex[:12]}"

        if not settings.ems_acquisition_model or not settings.llm_api_key:
            raise HTTPException(
                status_code=503,
                detail=(
                    "EMS acquisition model is not configured. Set AI_EMS_ACQUISITION_MODEL "
                    "or AI_EMS_OVERVIEW_MODEL with AI_LLM_API_KEY to enable editorial signal extraction."
                ),
            )

        parsed = self._call_llm(settings, request)
        signals = self._signals(parsed, request.max_signals)
        return EmsAcquisitionSignalResponse(
            request_id=request_id,
            generated_at=datetime.now(timezone.utc),
            service="ai",
            status="ok",
            model=settings.ems_acquisition_model,
            signals=signals,
        )

    def _call_llm(self, settings, request: EmsAcquisitionSignalRequest) -> dict:
        payload = {
            "model": settings.ems_acquisition_model,
            "temperature": 0.1,
            "response_format": {
                "type": "json_schema",
                "json_schema": {
                    "name": "ems_acquisition_signals",
                    "strict": True,
                    "schema": {
                        "type": "object",
                        "additionalProperties": False,
                        "required": ["signals"],
                        "properties": {
                            "signals": {
                                "type": "array",
                                "maxItems": request.max_signals,
                                "items": {
                                    "type": "object",
                                    "additionalProperties": False,
                                    "required": [
                                        "article_url",
                                        "article_title",
                                        "signal_type",
                                        "query",
                                        "confidence_score",
                                        "rationale",
                                    ],
                                    "properties": {
                                        "article_url": {"type": ["string", "null"]},
                                        "article_title": {"type": ["string", "null"]},
                                        "signal_type": {
                                            "type": "string",
                                            "enum": ["track", "artist", "playlist_query", "genre", "scene"],
                                        },
                                        "query": {"type": "string", "minLength": 1, "maxLength": 200},
                                        "confidence_score": {"type": "number", "minimum": 0, "maximum": 1},
                                        "rationale": {"type": "string", "maxLength": 500},
                                    },
                                },
                            }
                        },
                    },
                },
            },
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You extract music acquisition signals from editorial feed articles for a streaming "
                        "playlist collection engine. Return only provider-searchable music seeds. "
                        "Prefer concrete artist, track, genre, scene, or playlist search queries that can be "
                        "resolved by Spotify or TIDAL. Do not invent facts beyond the supplied article titles "
                        "and summaries. Use Korean, Japanese, or English titles as written when that improves "
                        "provider search. Return compact JSON matching the schema exactly."
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "source_name": request.source_name,
                            "source_url": request.source_url,
                            "source_weight": request.source_weight,
                            "max_signals": request.max_signals,
                            "articles": [article.model_dump(mode="json") for article in request.articles],
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
            with urllib_request.urlopen(http_request, timeout=30) as response:
                body = json.loads(response.read().decode("utf-8"))
        except HTTPError as exception:
            raise HTTPException(
                status_code=502,
                detail=f"EMS acquisition LLM request failed with status {exception.code}.",
            ) from exception
        except (URLError, TimeoutError) as exception:
            raise HTTPException(
                status_code=502,
                detail="EMS acquisition LLM endpoint is unreachable.",
            ) from exception

        content = (
            body.get("choices", [{}])[0]
            .get("message", {})
            .get("content")
        )
        if not content:
            raise HTTPException(status_code=502, detail="EMS acquisition LLM returned an empty message.")

        try:
            parsed = json.loads(content)
        except json.JSONDecodeError as exception:
            raise HTTPException(status_code=502, detail="EMS acquisition LLM returned invalid JSON.") from exception

        if not isinstance(parsed, dict):
            raise HTTPException(status_code=502, detail="EMS acquisition LLM returned an invalid JSON shape.")
        return parsed

    def _signals(self, parsed: dict, max_signals: int) -> list[EmsAcquisitionSignal]:
        raw_signals = parsed.get("signals")
        if not isinstance(raw_signals, list):
            raise HTTPException(status_code=502, detail="EMS acquisition LLM returned invalid `signals`.")
        try:
            return [EmsAcquisitionSignal.model_validate(item) for item in raw_signals[:max_signals]]
        except ValueError as exception:
            raise HTTPException(status_code=502, detail="EMS acquisition LLM returned invalid signal objects.") from exception
