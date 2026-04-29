from __future__ import annotations

from fastapi import APIRouter

from app.schemas.recommendation import (
    RecommendationPreviewRequest,
    RecommendationPreviewResponse,
)
from app.services.recommendation_service import RecommendationService

router = APIRouter(prefix="/v1/recommendations", tags=["recommendations"])

service = RecommendationService()


@router.post(
    "/preview",
    response_model=RecommendationPreviewResponse,
    summary="Generate a preview recommendation set",
)
def preview_recommendations(
    request: RecommendationPreviewRequest,
) -> RecommendationPreviewResponse:
    return service.generate_preview(request)
