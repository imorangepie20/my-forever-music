from __future__ import annotations

from pydantic import BaseModel


class EndpointLink(BaseModel):
    label: str
    path: str


class RootResponse(BaseModel):
    service: str
    status: str
    version: str
    environment: str
    endpoints: list[EndpointLink]


class HealthResponse(BaseModel):
    service: str
    status: str
    version: str
    environment: str
