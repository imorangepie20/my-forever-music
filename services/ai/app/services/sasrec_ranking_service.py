from __future__ import annotations

import json
from pathlib import Path

import torch

from app.models.sasrec import SasrecMvpModel
from app.schemas.sasrec import (
    SasrecRankedCandidate,
    SasrecRankingRequest,
    SasrecRankingResponse,
)


class SasrecRankingService:
    def __init__(self, artifact_dir: str | Path = "models") -> None:
        self._artifact_dir = Path(artifact_dir)

    def rank_candidates(self, request: SasrecRankingRequest) -> SasrecRankingResponse:
        artifact_dir = self._artifact_dir / "sasrec" / request.model_version
        model_path = artifact_dir / "model.pt"
        metadata_path = artifact_dir / "metadata.json"
        if not model_path.exists() or not metadata_path.exists():
            return SasrecRankingResponse(
                service="sasrec-ranking",
                status="model_not_found",
                model_version=request.model_version,
                ranked_candidates=[],
                warnings=[
                    "SASRec artifact was not found. Train the model with persist_artifact=true before ranking."
                ],
            )

        metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
        vocabulary = metadata.get("vocabulary", [])
        item_index_by_track_id = {
            item["track_id"]: int(item["item_index"])
            for item in vocabulary
            if item.get("track_id") and item.get("item_index")
        }
        track_id_by_item_index = {
            item_index: track_id
            for track_id, item_index in item_index_by_track_id.items()
        }
        training = metadata.get("training", {})
        max_context_length = int(training.get("max_context_length", 50))
        hidden_size = int(training.get("hidden_size", 32))
        model = SasrecMvpModel(
            vocabulary_size=len(item_index_by_track_id),
            max_context_length=max_context_length,
            hidden_size=hidden_size,
            attention_heads=self._resolve_attention_heads(hidden_size),
        )
        model.load_state_dict(torch.load(model_path, map_location="cpu"))
        model.eval()

        warnings: list[str] = []
        context_indices = [
            item_index_by_track_id[track_id]
            for track_id in request.context_track_ids
            if track_id in item_index_by_track_id
        ]
        if not context_indices:
            warnings.append("No context_track_ids matched the SASRec vocabulary.")
        if len(context_indices) != len(request.context_track_ids):
            warnings.append("Some context_track_ids were not present in the SASRec vocabulary.")

        candidate_indices = [
            item_index_by_track_id[track_id]
            for track_id in request.candidate_track_ids
            if track_id in item_index_by_track_id
        ]
        if not candidate_indices:
            warnings.append("No candidate_track_ids matched the SASRec vocabulary.")
        if len(candidate_indices) != len(request.candidate_track_ids):
            warnings.append("Some candidate_track_ids were not present in the SASRec vocabulary.")

        if not context_indices or not candidate_indices:
            return SasrecRankingResponse(
                service="sasrec-ranking",
                status="insufficient_known_items",
                model_version=request.model_version,
                ranked_candidates=[],
                warnings=warnings,
            )

        context_tensor = self._context_tensor(context_indices, max_context_length)
        with torch.no_grad():
            logits = model(context_tensor)[0]
            scores = [
                (item_index, float(logits[item_index].item()))
                for item_index in candidate_indices
            ]

        ranked = sorted(scores, key=lambda item: item[1], reverse=True)[:request.k]
        ranked_candidates = [
            SasrecRankedCandidate(
                rank=index + 1,
                track_id=track_id_by_item_index[item_index],
                item_index=item_index,
                score=round(score, 6),
            )
            for index, (item_index, score) in enumerate(ranked)
        ]
        return SasrecRankingResponse(
            service="sasrec-ranking",
            status="ok",
            model_version=request.model_version,
            ranked_candidates=ranked_candidates,
            warnings=warnings,
        )

    def _context_tensor(self, context_indices: list[int], max_context_length: int) -> torch.Tensor:
        context = context_indices[-max_context_length:]
        padding = [0] * (max_context_length - len(context))
        return torch.tensor([[*padding, *context]], dtype=torch.long)

    def _resolve_attention_heads(self, hidden_size: int) -> int:
        for candidate in (8, 4, 2):
            if hidden_size % candidate == 0:
                return candidate
        return 1
