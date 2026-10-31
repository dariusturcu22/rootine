import io
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from PIL import Image

from app.config import Settings
from app.main import create_app

MODEL_DIR = Path(__file__).resolve().parent.parent / "models" / "plant-v1"


@pytest.fixture
def client():
    settings = Settings(ai_mock=True, model_dir=MODEL_DIR)
    with TestClient(create_app(settings)) as test_client:
        yield test_client


@pytest.fixture
def png_bytes() -> bytes:
    buffer = io.BytesIO()
    Image.new("RGB", (64, 64), color=(30, 120, 40)).save(buffer, format="PNG")
    return buffer.getvalue()
