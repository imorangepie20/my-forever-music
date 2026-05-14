from __future__ import annotations

import json
import math
from pathlib import Path
from uuid import uuid5, NAMESPACE_URL

import torch
from torch import nn

from app.models.sasrec import SasrecMvpModel
from app.schemas.recommendation_dataset import RecommendationDatasetImportRequest
from app.schemas.sasrec import (
    SasrecEvaluationExample,
    SasrecMetricDelta,
    SasrecMetricQualification,
    SasrecModelArtifact,
    SasrecOfflineMetrics,
    SasrecTrainingExample,
    SasrecTrainingResponse,
    SasrecTrainingSummary,
)
from app.services.sasrec_dataset_service import SasrecDatasetService
from app.services.sasrec_offline_report_service import SasrecOfflineReportService

DEFAULT_EPOCHS = 30
DEFAULT_HIDDEN_SIZE = 32
DEFAULT_LEARNING_RATE = 0.01


class SasrecTrainingService:
    def __init__(
        self,
        dataset_service: SasrecDatasetService | None = None,
        artifact_dir: str | Path = "models",
    ) -> None:
        self._dataset_service = dataset_service or SasrecDatasetService()
        self._offline_report_service = SasrecOfflineReportService(self._dataset_service)
        self._artifact_dir = Path(artifact_dir)

    def train_mvp(
        self,
        request: RecommendationDatasetImportRequest,
        max_context_length: int = 50,
        k: int = 10,
        epochs: int = DEFAULT_EPOCHS,
        hidden_size: int = DEFAULT_HIDDEN_SIZE,
        learning_rate: float = DEFAULT_LEARNING_RATE,
        persist_artifact: bool = True,
    ) -> SasrecTrainingResponse:
        torch.manual_seed(42)
        resolved_k = max(1, min(100, k))
        resolved_epochs = max(1, min(200, epochs))
        resolved_hidden_size = max(8, min(128, hidden_size))
        resolved_context_length = max(1, min(50, max_context_length))
        dataset = self._dataset_service.prepare_dataset(
            request,
            max_context_length=resolved_context_length,
        )
        baseline_report = self._offline_report_service.build_report(
            request,
            max_context_length=resolved_context_length,
            k=resolved_k,
        )
        examples = dataset.training_examples
        train_examples = examples[:-1]
        evaluation_examples = examples[-1:] if examples else []
        warnings = list(dataset.warnings)

        final_loss = None
        predictions: list[SasrecEvaluationExample] = []
        artifact = SasrecModelArtifact(saved=False)
        model_version = self._model_version(request, len(dataset.vocabulary), len(train_examples))
        if not train_examples:
            warnings.append("SASRec MVP training skipped because there is no train split before evaluation.")
        elif not dataset.vocabulary:
            warnings.append("SASRec MVP training skipped because the vocabulary is empty.")
        else:
            model = SasrecMvpModel(
                vocabulary_size=len(dataset.vocabulary),
                max_context_length=resolved_context_length,
                hidden_size=resolved_hidden_size,
                attention_heads=self._resolve_attention_heads(resolved_hidden_size),
            )
            final_loss = self._train_model(
                model=model,
                examples=train_examples,
                max_context_length=resolved_context_length,
                epochs=resolved_epochs,
                learning_rate=learning_rate,
            )
            predictions = [
                self._evaluate_example(model, example, resolved_context_length, resolved_k)
                for example in evaluation_examples
            ]
            if persist_artifact:
                artifact = self._save_artifact(
                    model=model,
                    model_version=model_version,
                    request=request,
                    dataset=dataset,
                    max_context_length=resolved_context_length,
                    hidden_size=resolved_hidden_size,
                    epochs=resolved_epochs,
                    learning_rate=learning_rate,
                    final_loss=final_loss,
                )

        metrics = self._compute_metrics(predictions, resolved_k)
        metric_delta = self._metric_delta(metrics, baseline_report.metrics)
        qualification = self._evaluate_qualification(
            metric_delta,
            train_example_count=len(train_examples),
            evaluation_example_count=len(predictions),
        )
        if evaluation_examples and not predictions:
            warnings.append("SASRec MVP evaluation skipped because no model was trained.")
        if not qualification.qualified:
            warnings.append(qualification.reason)

        return SasrecTrainingResponse(
            service="sasrec-mvp-training",
            status="ok",
            user_id=request.user_id,
            model_version=model_version,
            summary=SasrecTrainingSummary(
                vocabulary_size=len(dataset.vocabulary),
                train_example_count=len(train_examples),
                evaluation_example_count=len(evaluation_examples),
                max_context_length=resolved_context_length,
                hidden_size=resolved_hidden_size,
                epoch_count=resolved_epochs if train_examples else 0,
                final_loss=None if final_loss is None else round(final_loss, 6),
            ),
            metrics=metrics,
            baseline_metrics=baseline_report.metrics,
            metric_delta=metric_delta,
            qualification=qualification,
            model_artifact=artifact,
            evaluation_examples=predictions,
            warnings=warnings,
        )

    def _train_model(
        self,
        model: SasrecMvpModel,
        examples: list[SasrecTrainingExample],
        max_context_length: int,
        epochs: int,
        learning_rate: float,
    ) -> float:
        model.train()
        optimizer = torch.optim.Adam(model.parameters(), lr=learning_rate)
        context_tensor = self._context_tensor(examples, max_context_length)
        target_tensor = torch.tensor([example.target_item_index for example in examples], dtype=torch.long)
        weight_tensor = torch.tensor([example.weight for example in examples], dtype=torch.float32)
        loss_fn = nn.CrossEntropyLoss(reduction="none")
        final_loss = 0.0

        for _ in range(epochs):
            optimizer.zero_grad()
            logits = model(context_tensor)
            losses = loss_fn(logits, target_tensor)
            weighted_loss = (losses * weight_tensor).mean()
            weighted_loss.backward()
            optimizer.step()
            final_loss = float(weighted_loss.detach().item())

        return final_loss

    def _evaluate_example(
        self,
        model: SasrecMvpModel,
        example: SasrecTrainingExample,
        max_context_length: int,
        k: int,
    ) -> SasrecEvaluationExample:
        model.eval()
        with torch.no_grad():
            context_tensor = self._context_tensor([example], max_context_length)
            logits = model(context_tensor)[0]
            logits[0] = -torch.inf
            top_indices = torch.topk(logits, k=min(k, logits.shape[0] - 1)).indices.tolist()

        hit_rank = None
        for index, item_index in enumerate(top_indices, start=1):
            if item_index == example.target_item_index:
                hit_rank = index
                break

        return SasrecEvaluationExample(
            target_track_id=example.target_track_id,
            target_item_index=example.target_item_index,
            predicted_item_indices=top_indices,
            hit_rank=hit_rank,
        )

    def _context_tensor(
        self,
        examples: list[SasrecTrainingExample],
        max_context_length: int,
    ) -> torch.Tensor:
        rows: list[list[int]] = []
        for example in examples:
            context = example.context_item_indices[-max_context_length:]
            padding = [0] * (max_context_length - len(context))
            rows.append([*padding, *context])
        return torch.tensor(rows, dtype=torch.long)

    def _compute_metrics(
        self,
        predictions: list[SasrecEvaluationExample],
        k: int,
    ) -> SasrecOfflineMetrics:
        if not predictions:
            return SasrecOfflineMetrics(hit_rate_at_k=0.0, mrr_at_k=0.0, ndcg_at_k=0.0)

        hit_count = 0
        reciprocal_rank_sum = 0.0
        ndcg_sum = 0.0
        for prediction in predictions:
            if prediction.hit_rank is None or prediction.hit_rank > k:
                continue
            hit_count += 1
            reciprocal_rank_sum += 1.0 / prediction.hit_rank
            ndcg_sum += 1.0 / math.log2(prediction.hit_rank + 1)

        total = len(predictions)
        return SasrecOfflineMetrics(
            hit_rate_at_k=round(hit_count / total, 4),
            mrr_at_k=round(reciprocal_rank_sum / total, 4),
            ndcg_at_k=round(ndcg_sum / total, 4),
        )

    def _resolve_attention_heads(self, hidden_size: int) -> int:
        for candidate in (8, 4, 2):
            if hidden_size % candidate == 0:
                return candidate
        return 1

    def _save_artifact(
        self,
        model: SasrecMvpModel,
        model_version: str,
        request: RecommendationDatasetImportRequest,
        dataset,
        max_context_length: int,
        hidden_size: int,
        epochs: int,
        learning_rate: float,
        final_loss: float,
    ) -> SasrecModelArtifact:
        artifact_dir = self._artifact_dir / "sasrec" / model_version
        artifact_dir.mkdir(parents=True, exist_ok=True)
        model_path = artifact_dir / "model.pt"
        metadata_path = artifact_dir / "metadata.json"
        torch.save(model.state_dict(), model_path)
        metadata = {
            "model_version": model_version,
            "user_id": request.user_id,
            "generated_at": request.generated_at.isoformat(),
            "dataset_version": request.summary.dataset_version,
            "dataset_fingerprint": request.summary.dataset_fingerprint,
            "source_dataset": request.summary.model_dump(),
            "vocabulary": [item.model_dump() for item in dataset.vocabulary],
            "summary": dataset.summary.model_dump(),
            "training": {
                "max_context_length": max_context_length,
                "hidden_size": hidden_size,
                "epochs": epochs,
                "learning_rate": learning_rate,
                "final_loss": final_loss,
            },
        }
        metadata_path.write_text(json.dumps(metadata, indent=2, sort_keys=True), encoding="utf-8")
        return SasrecModelArtifact(
            artifact_dir=str(artifact_dir),
            model_path=str(model_path),
            metadata_path=str(metadata_path),
            saved=True,
        )

    def _metric_delta(
        self,
        metrics: SasrecOfflineMetrics,
        baseline_metrics: SasrecOfflineMetrics,
    ) -> SasrecMetricDelta:
        return SasrecMetricDelta(
            hit_rate_at_k=round(metrics.hit_rate_at_k - baseline_metrics.hit_rate_at_k, 4),
            mrr_at_k=round(metrics.mrr_at_k - baseline_metrics.mrr_at_k, 4),
            ndcg_at_k=round(metrics.ndcg_at_k - baseline_metrics.ndcg_at_k, 4),
        )

    def _evaluate_qualification(
        self,
        delta: SasrecMetricDelta,
        train_example_count: int,
        evaluation_example_count: int,
    ) -> SasrecMetricQualification:
        threshold = 0.0
        if train_example_count == 0 or evaluation_example_count == 0:
            return SasrecMetricQualification(
                qualified=False,
                threshold=threshold,
                reason="SASRec was not trained or evaluated (insufficient data).",
            )

        regressions: list[str] = []
        if delta.hit_rate_at_k < threshold:
            regressions.append(f"hit_rate_at_k {delta.hit_rate_at_k:+.4f}")
        if delta.mrr_at_k < threshold:
            regressions.append(f"mrr_at_k {delta.mrr_at_k:+.4f}")
        if delta.ndcg_at_k < threshold:
            regressions.append(f"ndcg_at_k {delta.ndcg_at_k:+.4f}")

        if regressions:
            return SasrecMetricQualification(
                qualified=False,
                threshold=threshold,
                reason=f"SASRec underperforms recency baseline on {', '.join(regressions)}.",
            )

        improvements = [
            value
            for value in (delta.hit_rate_at_k, delta.mrr_at_k, delta.ndcg_at_k)
            if value > threshold
        ]
        if improvements:
            return SasrecMetricQualification(
                qualified=True,
                threshold=threshold,
                reason="SASRec improves at least one metric over recency baseline without regression.",
            )
        return SasrecMetricQualification(
            qualified=True,
            threshold=threshold,
            reason="SASRec matches recency baseline on all metrics (no regression, no improvement).",
        )

    def _model_version(
        self,
        request: RecommendationDatasetImportRequest,
        vocabulary_size: int,
        train_example_count: int,
    ) -> str:
        seed = f"{request.user_id}:{request.generated_at.isoformat()}:{vocabulary_size}:{train_example_count}"
        return f"sasrec-mvp-{uuid5(NAMESPACE_URL, seed).hex[:12]}"
