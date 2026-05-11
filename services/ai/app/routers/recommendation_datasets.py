from __future__ import annotations

from fastapi import APIRouter

from app.schemas.recommendation_dataset import (
    RecommendationDatasetImportRequest,
    RecommendationDatasetValidationResponse,
)
from app.schemas.sasrec import SasrecDatasetResponse
from app.services.recommendation_dataset_service import RecommendationDatasetService
from app.services.sasrec_dataset_service import SasrecDatasetService

router = APIRouter(prefix="/v1/recommendations/datasets", tags=["recommendation-datasets"])

service = RecommendationDatasetService()
sasrec_service = SasrecDatasetService()


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
