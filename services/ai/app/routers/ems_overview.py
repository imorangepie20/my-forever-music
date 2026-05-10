from __future__ import annotations

from fastapi import APIRouter

from app.schemas.ems_overview import EmsOverviewRequest, EmsOverviewResponse
from app.services.ems_overview_service import EmsOverviewService

router = APIRouter(prefix="/v1/ems", tags=["ems"])

service = EmsOverviewService()


@router.post(
    "/overview",
    response_model=EmsOverviewResponse,
    summary="Interpret deterministic EMS overview context with an LLM",
)
def interpret_ems_overview(request: EmsOverviewRequest) -> EmsOverviewResponse:
    return service.interpret(request)
