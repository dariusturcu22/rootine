from pathlib import Path

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Settings read from environment variables (AI_MOCK, MODEL_DIR, ...)."""

    ai_mock: bool = False
    model_dir: Path = Path("models/plant-v1")
    open_meteo_url: str = "https://api.open-meteo.com/v1/forecast"
    cache_ttl: int = 3600
