from __future__ import annotations

import json
from pathlib import Path

from app.schemas.sasrec import SasrecModelRegistryResponse


REGISTRY_FILE_NAME = "registry.json"


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

        record = self._user_record(self._load_registry(), user_id) if user_id else {}
        promoted = record.get("promoted_version")
        disabled = set(record.get("disabled_versions", []))

        if user_id and promoted and promoted not in disabled and self._artifact_exists(promoted):
            metadata = self._read_metadata(promoted) or {}
            return self._snapshot(user_id, promoted, metadata, [])

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
            model_version = metadata.get("model_version") or metadata_path.parent.name
            if model_version in disabled:
                continue
            generated_at = metadata.get("generated_at")
            candidates.append(
                {
                    "metadata": metadata,
                    "artifact_dir": metadata_path.parent,
                    "model_version": model_version,
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
        return self._snapshot(user_id, latest["model_version"], latest["metadata"], warnings)

    def promote(self, user_id: str, model_version: str) -> SasrecModelRegistryResponse:
        if not self._artifact_exists(model_version):
            return SasrecModelRegistryResponse(
                service="sasrec-model-registry",
                status="not_found",
                user_id=user_id,
                warnings=[f"SASRec artifact {model_version} does not exist."],
            )
        metadata = self._read_metadata(model_version) or {}
        artifact_user = metadata.get("user_id")
        if artifact_user and artifact_user != user_id:
            return SasrecModelRegistryResponse(
                service="sasrec-model-registry",
                status="forbidden",
                user_id=user_id,
                warnings=[f"SASRec artifact {model_version} belongs to user {artifact_user}."],
            )

        registry = self._load_registry()
        record = self._user_record(registry, user_id)
        previous = record.get("promoted_version")
        if previous and previous != model_version:
            history = record.setdefault("previous_versions", [])
            if previous in history:
                history.remove(previous)
            history.append(previous)
        record["promoted_version"] = model_version
        disabled = record.setdefault("disabled_versions", [])
        if model_version in disabled:
            disabled.remove(model_version)
        self._save_registry(registry)
        return self._snapshot(user_id, model_version, metadata, [])

    def disable(self, user_id: str, model_version: str) -> SasrecModelRegistryResponse:
        registry = self._load_registry()
        record = self._user_record(registry, user_id)
        disabled = record.setdefault("disabled_versions", [])
        if model_version not in disabled:
            disabled.append(model_version)
        if record.get("promoted_version") == model_version:
            history = record.setdefault("previous_versions", [])
            record["promoted_version"] = history.pop() if history else None
        self._save_registry(registry)
        return self.latest_model(user_id=user_id)

    def rollback(self, user_id: str) -> SasrecModelRegistryResponse:
        registry = self._load_registry()
        record = self._user_record(registry, user_id)
        history = record.setdefault("previous_versions", [])
        if not history:
            return SasrecModelRegistryResponse(
                service="sasrec-model-registry",
                status="not_found",
                user_id=user_id,
                warnings=["No previous SASRec model in rollback history."],
            )
        record["promoted_version"] = history.pop()
        self._save_registry(registry)
        return self.latest_model(user_id=user_id)

    def _registry_path(self) -> Path:
        return self._artifact_dir / "sasrec" / REGISTRY_FILE_NAME

    def _load_registry(self) -> dict:
        path = self._registry_path()
        if not path.exists():
            return {"users": {}}
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            return {"users": {}}

    def _save_registry(self, registry: dict) -> None:
        path = self._registry_path()
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(registry, indent=2), encoding="utf-8")

    def _user_record(self, registry: dict, user_id: str | None) -> dict:
        if not user_id:
            return {}
        users = registry.setdefault("users", {})
        return users.setdefault(user_id, {
            "promoted_version": None,
            "previous_versions": [],
            "disabled_versions": [],
        })

    def _artifact_exists(self, model_version: str) -> bool:
        directory = self._artifact_dir / "sasrec" / model_version
        return (directory / "metadata.json").exists() and (directory / "model.pt").exists()

    def _read_metadata(self, model_version: str) -> dict | None:
        path = self._artifact_dir / "sasrec" / model_version / "metadata.json"
        if not path.exists():
            return None
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            return None

    def _snapshot(
        self,
        user_id: str | None,
        model_version: str,
        metadata: dict,
        warnings: list[str],
    ) -> SasrecModelRegistryResponse:
        summary = metadata.get("summary", {})
        training = metadata.get("training", {})
        artifact_dir = self._artifact_dir / "sasrec" / model_version
        return SasrecModelRegistryResponse(
            service="sasrec-model-registry",
            status="ok",
            user_id=metadata.get("user_id") or user_id,
            model_version=model_version,
            artifact_dir=str(artifact_dir),
            generated_at=metadata.get("generated_at"),
            vocabulary_size=summary.get("unique_track_count"),
            train_example_count=summary.get("training_example_count") or training.get("train_example_count"),
            dataset_version=metadata.get("dataset_version") or summary.get("dataset_version"),
            dataset_fingerprint=metadata.get("dataset_fingerprint") or summary.get("dataset_fingerprint"),
            warnings=list(warnings),
        )
