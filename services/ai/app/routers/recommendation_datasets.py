from __future__ import annotations

from fastapi import APIRouter

from app.schemas.recommendation_dataset import (
    RecommendationDatasetImportRequest,
    RecommendationDatasetValidationResponse,
)
from app.services.recommendation_dataset_service import RecommendationDatasetService

router = APIRouter(prefix="/v1/recommendations/datasets", tags=["recommendation-datasets"])

service = RecommendationDatasetService()


@router.post(
    "/validate",
    response_model=RecommendationDatasetValidationResponse,
    summary="Validate an exported recommendation training dataset",
)
def validate_recommendation_dataset(
    request: RecommendationDatasetImportRequest,
) -> RecommendationDatasetValidationResponse:
    return service.validate_import(request)
