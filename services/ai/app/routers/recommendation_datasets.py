from __future__ import annotations

from fastapi import APIRouter

from app.schemas.recommendation_dataset import (
    RecommendationDatasetImportRequest,
    RecommendationDatasetValidationResponse,
)
from app.schemas.sasrec import SasrecDatasetResponse, SasrecOfflineReportResponse
from app.schemas.sasrec import SasrecTrainingResponse
from app.services.recommendation_dataset_service import RecommendationDatasetService
from app.services.sasrec_dataset_service import SasrecDatasetService
from app.services.sasrec_offline_report_service import SasrecOfflineReportService
from app.services.sasrec_training_service import SasrecTrainingService

router = APIRouter(prefix="/v1/recommendations/datasets", tags=["recommendation-datasets"])

service = RecommendationDatasetService()
sasrec_service = SasrecDatasetService()
offline_report_service = SasrecOfflineReportService(sasrec_service)
training_service = SasrecTrainingService(sasrec_service)


@router.post(
    "/validate",
    response_model=RecommendationDatasetValidationResponse,
    summary="Validate an exported recommendation training dataset",
)
def validate_recommendation_dataset(
    request: RecommendationDatasetImportRequest,
) -> RecommendationDatasetValidationResponse:
    return service.validate_import(request)


@router.post(
    "/sasrec/prepare",
    response_model=SasrecDatasetResponse,
    summary="Prepare next-item training windows for a SASRec MVP",
)
def prepare_sasrec_dataset(
    request: RecommendationDatasetImportRequest,
    max_context_length: int = 50,
) -> SasrecDatasetResponse:
    return sasrec_service.prepare_dataset(request, max_context_length=max_context_length)


@router.post(
    "/sasrec/offline-report",
    response_model=SasrecOfflineReportResponse,
    summary="Build a leave-last-out offline metric report for SASRec dataset windows",
)
def build_sasrec_offline_report(
    request: RecommendationDatasetImportRequest,
    max_context_length: int = 50,
    k: int = 10,
) -> SasrecOfflineReportResponse:
    return offline_report_service.build_report(
        request,
        max_context_length=max_context_length,
        k=k,
    )


@router.post(
    "/sasrec/train",
    response_model=SasrecTrainingResponse,
    summary="Train a minimal PyTorch SASRec MVP and evaluate it with leave-last-out metrics",
)
def train_sasrec_mvp(
    request: RecommendationDatasetImportRequest,
    max_context_length: int = 50,
    k: int = 10,
    epochs: int = 30,
    hidden_size: int = 32,
    learning_rate: float = 0.01,
) -> SasrecTrainingResponse:
    return training_service.train_mvp(
        request,
        max_context_length=max_context_length,
        k=k,
        epochs=epochs,
        hidden_size=hidden_size,
        learning_rate=learning_rate,
    )
