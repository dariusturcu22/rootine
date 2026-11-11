import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import Any

from fastapi import FastAPI

from app.api import crops, health, predict, risk, threat
from app.config import Settings
from app.inference.classifier import (
    Classifier,
    Label,
    create_inference_session,
    load_crops,
    load_labels,
    load_metadata,
)
from app.weather.open_meteo_client import OpenMeteoClient

logger = logging.getLogger("rootine.ai-service")

MODEL_RELEASE_TAG = "v0.1.0-demo1"


def load_classifier(
    settings: Settings, labels: list[Label], metadata: dict[str, Any]
) -> Classifier | None:
    if settings.ai_mock:
        logger.warning("AI_MOCK=true: mock mode, predictions are fixed and no model is loaded")
        return None
    model_path = settings.model_dir / metadata["model_file"]
    if not model_path.exists():
        raise RuntimeError(
            f"AI_MOCK=false but the model file {model_path} is missing. "
            f"Run 'python scripts/download_model.py --tag {MODEL_RELEASE_TAG}' in ai-service/ "
            "or set AI_MOCK=true."
        )
    logger.warning("AI_MOCK=false: loading model %s", model_path)
    return Classifier(create_inference_session(model_path), labels, metadata, settings)


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or Settings()

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        model_dir = settings.model_dir
        app.state.settings = settings
        app.state.labels = load_labels(model_dir / "labels.csv")
        app.state.crops = load_crops(model_dir / "crops.json")
        app.state.metadata = load_metadata(model_dir / "metadata.json")
        app.state.weather_client = OpenMeteoClient(
            base_url=settings.open_meteo_url,
            archive_url=settings.open_meteo_archive_url,
            cache_ttl=settings.cache_ttl,
        )
        app.state.mock = settings.ai_mock
        app.state.classifier = load_classifier(settings, app.state.labels, app.state.metadata)
        try:
            yield
        finally:
            await app.state.weather_client.aclose()

    app = FastAPI(title="Rootine AI service", version="0.1.0", lifespan=lifespan)
    for module in (health, crops, predict, risk, threat):
        app.include_router(module.router)
    return app


app = create_app()
