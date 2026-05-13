from __future__ import annotations

from fastapi import APIRouter

from app.schemas.ems_acquisition import EmsAcquisitionSignalRequest, EmsAcquisitionSignalResponse
from app.services.ems_acquisition_service import EmsAcquisitionService

router = APIRouter(prefix="/v1/ems/acquisition", tags=["ems"])

service = EmsAcquisitionService()


@router.post(
    "/signals",
    response_model=EmsAcquisitionSignalResponse,
    summary="Extract EMS editorial acquisition signals with the configured model",
)
def extract_ems_acquisition_signals(request: EmsAcquisitionSignalRequest) -> EmsAcquisitionSignalResponse:
    return service.extract_signals(request)
