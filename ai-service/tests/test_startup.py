import pytest
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app


def test_startup_without_model_fails_when_mock_is_off(model_dir_without_model):
    settings = Settings(ai_mock=False, model_dir=model_dir_without_model)

    with pytest.raises(RuntimeError, match="download_model.py --tag v0.1.0-demo1"):
        with TestClient(create_app(settings)):
            pass


def test_startup_without_model_works_in_mock_mode(model_dir_without_model):
    settings = Settings(ai_mock=True, model_dir=model_dir_without_model)

    with TestClient(create_app(settings)) as test_client:
        assert test_client.get("/health").json()["mock"] is True
