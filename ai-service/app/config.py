from pathlib import Path

from pydantic_settings import BaseSettings

BYTES_PER_MEGABYTE = 1024 * 1024
MAX_UPLOAD_MEGABYTES = 10


class Settings(BaseSettings):
    """Settings read from environment variables (AI_MOCK, MODEL_DIR, ...)."""

    ai_mock: bool = False
    model_dir: Path = Path("models/plant-v1")
    open_meteo_url: str = "https://api.open-meteo.com/v1/forecast"
    open_meteo_archive_url: str = "https://archive-api.open-meteo.com/v1/archive"
    cache_ttl: int = 3600
    crop_match_threshold: float = 0.5
    default_confidence_threshold: float = 0.60
    max_upload_bytes: int = MAX_UPLOAD_MEGABYTES * BYTES_PER_MEGABYTE
