from __future__ import annotations

from fastapi import APIRouter

from app.config import get_settings
from app.schemas.health import EndpointLink, HealthResponse, RootResponse

router = APIRouter(tags=["system"])


def _public_path(root_path: str, path: str) -> str:
    prefix = root_path.rstrip("/")
    return f"{prefix}{path}" if prefix else path


@router.get("/", response_model=RootResponse, summary="AI service information")
def read_root() -> RootResponse:
    settings = get_settings()
    return RootResponse(
        service="ai",
        status="ok",
        version=settings.app_version,
        environment=settings.environment,
        endpoints=[
            EndpointLink(label="health", path=_public_path(settings.root_path, "/health")),
            EndpointLink(label="docs", path=_public_path(settings.root_path, "/docs")),
            EndpointLink(label="openapi", path=_public_path(settings.root_path, "/openapi.json")),
        ],
    )


@router.get("/health", response_model=HealthResponse, summary="AI health check")
def read_health() -> HealthResponse:
    settings = get_settings()
    return HealthResponse(
        service="ai",
        status="ok",
        version=settings.app_version,
        environment=settings.environment,
    )
