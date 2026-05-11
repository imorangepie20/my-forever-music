from __future__ import annotations

import math

from app.schemas.recommendation_dataset import RecommendationDatasetImportRequest
from app.schemas.sasrec import (
    SasrecEvaluationExample,
    SasrecOfflineMetrics,
    SasrecOfflineReportResponse,
    SasrecTrainingExample,
)
from app.services.sasrec_dataset_service import SasrecDatasetService

DEFAULT_K = 10


class SasrecOfflineReportService:
    def __init__(self, dataset_service: SasrecDatasetService | None = None) -> None:
        self._dataset_service = dataset_service or SasrecDatasetService()

    def build_report(
        self,
        request: RecommendationDatasetImportRequest,
        max_context_length: int = 50,
        k: int = DEFAULT_K,
    ) -> SasrecOfflineReportResponse:
        resolved_k = max(1, min(100, k))
        dataset = self._dataset_service.prepare_dataset(
            request,
            max_context_length=max_context_length,
        )
        examples = dataset.training_examples
        train_examples = examples[:-1]
        evaluation_examples = examples[-1:] if examples else []
        predictions = [
            self._evaluate_example(example, dataset.vocabulary, resolved_k)
            for example in evaluation_examples
        ]
        metrics = self._compute_metrics(predictions, resolved_k)
        warnings = list(dataset.warnings)
        if not train_examples:
            warnings.append("Offline report has no train split before the evaluation example.")
        if not evaluation_examples:
            warnings.append("Offline report has no evaluation example.")

        return SasrecOfflineReportResponse(
            service="sasrec-offline-report",
            status="ok",
            user_id=request.user_id,
            k=resolved_k,
            train_example_count=len(train_examples),
            evaluation_example_count=len(evaluation_examples),
            metrics=metrics,
            evaluation_examples=predictions,
            warnings=warnings,
        )

    def _evaluate_example(
        self,
        example: SasrecTrainingExample,
        vocabulary,
        k: int,
    ) -> SasrecEvaluationExample:
        predicted_item_indices = self._rank_candidates(example, vocabulary, k)
        hit_rank = None
        for index, item_index in enumerate(predicted_item_indices, start=1):
            if item_index == example.target_item_index:
                hit_rank = index
                break

        return SasrecEvaluationExample(
            target_track_id=example.target_track_id,
            target_item_index=example.target_item_index,
            predicted_item_indices=predicted_item_indices,
            hit_rank=hit_rank,
        )

    def _rank_candidates(
        self,
        example: SasrecTrainingExample,
        vocabulary,
        k: int,
    ) -> list[int]:
        recency_ranked = list(dict.fromkeys(reversed(example.context_item_indices)))
        known_indices = {item.item_index for item in vocabulary}
        remaining = sorted(known_indices.difference(recency_ranked))
        return [*recency_ranked, *remaining][:k]

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
