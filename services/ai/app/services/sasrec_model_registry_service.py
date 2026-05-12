from __future__ import annotations

import json
from pathlib import Path

from app.schemas.sasrec import SasrecModelRegistryResponse


class SasrecModelRegistryService:
    def __init__(self, artifact_dir: str | Path = "models") -> None:
        self._artifact_dir = Path(artifact_dir)

    def latest_model(self, user_id: str | None = None) -> SasrecModelRegistryResponse:
        sasrec_dir = self._artifact_dir / "sasrec"
        if not sasrec_dir.exists():
            return SasrecModelRegistryResponse(
                service="sasrec-model-registry",
                status="not_found",
                user_id=user_id,
                warnings=["No SASRec artifact directory exists yet."],
            )

        candidates = []
        warnings: list[str] = []
        for metadata_path in sasrec_dir.glob("*/metadata.json"):
            model_path = metadata_path.parent / "model.pt"
            if not model_path.exists():
                warnings.append(f"Skipping {metadata_path.parent.name} because model.pt is missing.")
                continue

            try:
                metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
            except json.JSONDecodeError:
                warnings.append(f"Skipping {metadata_path.parent.name} because metadata.json is unreadable.")
                continue

            metadata_user_id = metadata.get("user_id")
            if user_id and metadata_user_id != user_id:
                continue

            generated_at = metadata.get("generated_at")
            candidates.append(
                {
                    "metadata": metadata,
                    "artifact_dir": metadata_path.parent,
                    "sort_key": (generated_at or "", metadata_path.stat().st_mtime),
                }
            )

        if not candidates:
            return SasrecModelRegistryResponse(
                service="sasrec-model-registry",
                status="not_found",
                user_id=user_id,
                warnings=[*warnings, "No matching persisted SASRec model artifact was found."],
            )

        latest = sorted(candidates, key=lambda item: item["sort_key"], reverse=True)[0]
        metadata = latest["metadata"]
        summary = metadata.get("summary", {})
        training = metadata.get("training", {})
        return SasrecModelRegistryResponse(
            service="sasrec-model-registry",
            status="ok",
            user_id=metadata.get("user_id"),
            model_version=metadata.get("model_version"),
            artifact_dir=str(latest["artifact_dir"]),
            generated_at=metadata.get("generated_at"),
            vocabulary_size=summary.get("unique_track_count"),
            train_example_count=summary.get("training_example_count") or training.get("train_example_count"),
            warnings=warnings,
        )
