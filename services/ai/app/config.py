from __future__ import annotations

from dataclasses import dataclass
from functools import lru_cache
import os


@dataclass(frozen=True)
class Settings:
    app_name: str
    app_version: str
    environment: str
    root_path: str


@lru_cache
def get_settings() -> Settings:
    return Settings(
        app_name=os.getenv("AI_APP_NAME", "My Forever Music AI"),
        app_version=os.getenv("AI_APP_VERSION", os.getenv("APP_VERSION", "0.1.0")),
        environment=os.getenv("AI_ENV", "local"),
        root_path=os.getenv("AI_ROOT_PATH", ""),
    )
