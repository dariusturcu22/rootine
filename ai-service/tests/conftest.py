import io
import shutil
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from PIL import Image

from app.config import Settings
from app.main import create_app
from tests.fakes import TOMATO_EARLY_BLIGHT, FakeSession, logits_favouring

MODEL_DIR = Path(__file__).resolve().parent.parent / "models" / "plant-v1"
MODEL_DESCRIPTION_FILES = ("labels.csv", "crops.json", "metadata.json")


@pytest.fixture
def client():
    settings = Settings(ai_mock=True, model_dir=MODEL_DIR)
    with TestClient(create_app(settings)) as test_client:
        yield test_client


@pytest.fixture
def model_dir_without_model(tmp_path):
    for file_name in MODEL_DESCRIPTION_FILES:
        shutil.copy(MODEL_DIR / file_name, tmp_path / file_name)
    return tmp_path


@pytest.fixture
def fake_session():
    return FakeSession(logits_favouring(TOMATO_EARLY_BLIGHT))


@pytest.fixture
def classifier_client(model_dir_without_model, fake_session, monkeypatch):
    (model_dir_without_model / "model.onnx").write_bytes(b"placeholder model file")
    monkeypatch.setattr("app.main.create_inference_session", lambda model_path: fake_session)
    settings = Settings(ai_mock=False, model_dir=model_dir_without_model)
    with TestClient(create_app(settings)) as test_client:
        yield test_client


@pytest.fixture
def png_bytes() -> bytes:
    buffer = io.BytesIO()
    Image.new("RGB", (64, 64), color=(30, 120, 40)).save(buffer, format="PNG")
    return buffer.getvalue()
