from __future__ import annotations

from app.schemas.recommendation_dataset import RecommendationDatasetImportRequest
from app.schemas.sasrec import (
    SasrecDatasetResponse,
    SasrecDatasetSummary,
    SasrecTrainingExample,
    SasrecVocabularyItem,
)

DEFAULT_MAX_CONTEXT_LENGTH = 50

POSITIVE_SIGNAL_WEIGHT = {
    "track_saved": 1.0,
    "added_to_playlist": 1.0,
    "recommendation_liked": 1.0,
    "play_completed": 0.8,
    "repeat_played": 0.9,
    "replay": 0.9,
}

NEGATIVE_SIGNAL_WEIGHT = {
    "recommendation_rejected": 0.2,
    "ignored_recommendation": 0.3,
    "skipped_early": 0.35,
    "skip_next": 0.4,
    "stopped_midway": 0.45,
}


class SasrecDatasetService:
    def prepare_dataset(
        self,
        request: RecommendationDatasetImportRequest,
        max_context_length: int = DEFAULT_MAX_CONTEXT_LENGTH,
    ) -> SasrecDatasetResponse:
        resolved_context_length = max(1, min(DEFAULT_MAX_CONTEXT_LENGTH, max_context_length))
        sorted_sequence = sorted(request.sequence, key=lambda item: item.occurred_at)
        usable_sequence = [
            item
            for item in sorted_sequence
            if item.track_id is not None and item.track_id.strip()
        ]
        vocabulary = self._build_vocabulary(usable_sequence)
        item_index_by_track_id = {
            item.track_id: item.item_index
            for item in vocabulary
        }
        training_examples = self._build_training_examples(
            usable_sequence,
            item_index_by_track_id,
            resolved_context_length,
        )
        warnings = self._build_warnings(request, usable_sequence, training_examples)

        return SasrecDatasetResponse(
            service="sasrec-dataset",
            status="ok",
            user_id=request.user_id,
            summary=SasrecDatasetSummary(
                sequence_item_count=len(request.sequence),
                usable_item_count=len(usable_sequence),
                unique_track_count=len(vocabulary),
                training_example_count=len(training_examples),
                max_context_length=resolved_context_length,
            ),
            vocabulary=vocabulary,
            training_examples=training_examples,
            warnings=warnings,
        )

    def _build_vocabulary(self, usable_sequence) -> list[SasrecVocabularyItem]:
        item_index_by_track_id: dict[str, int] = {}
        for item in usable_sequence:
            track_id = item.track_id.strip()
            if track_id not in item_index_by_track_id:
                item_index_by_track_id[track_id] = len(item_index_by_track_id) + 1

        return [
            SasrecVocabularyItem(track_id=track_id, item_index=item_index)
            for track_id, item_index in item_index_by_track_id.items()
        ]

    def _build_training_examples(
        self,
        usable_sequence,
        item_index_by_track_id: dict[str, int],
        max_context_length: int,
    ) -> list[SasrecTrainingExample]:
        examples: list[SasrecTrainingExample] = []
        context: list[int] = []

        for item in usable_sequence:
            track_id = item.track_id.strip()
            item_index = item_index_by_track_id[track_id]
            if context:
                examples.append(
                    SasrecTrainingExample(
                        context_item_indices=context[-max_context_length:],
                        target_item_index=item_index,
                        target_track_id=track_id,
                        source_token=item.token,
                        weight=self._resolve_weight(item),
                    )
                )
            context.append(item_index)

        return examples

    def _resolve_weight(self, item) -> float:
        if item.token.startswith("event:"):
            event_type = item.token.split(":", 2)[1]
            if event_type in POSITIVE_SIGNAL_WEIGHT:
                return POSITIVE_SIGNAL_WEIGHT[event_type]
            if event_type in NEGATIVE_SIGNAL_WEIGHT:
                return NEGATIVE_SIGNAL_WEIGHT[event_type]
        if item.weight is None:
            return 0.5
        return max(0.0, min(1.0, item.weight))

    def _build_warnings(
        self,
        request: RecommendationDatasetImportRequest,
        usable_sequence,
        training_examples: list[SasrecTrainingExample],
    ) -> list[str]:
        warnings: list[str] = []
        if len(usable_sequence) != len(request.sequence):
            warnings.append("Some sequence items were excluded because track_id was missing.")
        if len(usable_sequence) < 2:
            warnings.append("At least two usable sequence items are required for next-item training examples.")
        if not training_examples:
            warnings.append("No SASRec training examples were generated from this dataset.")
        return warnings
