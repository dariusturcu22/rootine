import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api import crops, health, predict, risk, threat
from app.config import Settings
from app.inference.classifier import Classifier, load_crops, load_labels, load_metadata

logger = logging.getLogger("rootine.ai")


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or Settings()

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        model_dir = settings.model_dir
        app.state.settings = settings
        app.state.labels = load_labels(model_dir / "labels.csv")
        app.state.crops = load_crops(model_dir / "crops.json")
        app.state.metadata = load_metadata(model_dir / "metadata.json")

        model_path = model_dir / app.state.metadata["model_file"]
        app.state.mock = settings.ai_mock or not model_path.exists()
        app.state.classifier = (
            None if app.state.mock else Classifier(model_path, app.state.metadata)
        )
        logger.info("AI service started, mock mode: %s", app.state.mock)
        yield

    app = FastAPI(title="Rootine AI service", version="0.1.0", lifespan=lifespan)
    for module in (health, crops, predict, risk, threat):
        app.include_router(module.router)
    return app


app = create_app()
