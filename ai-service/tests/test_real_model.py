import pytest

from app.config import Settings
from app.inference.classifier import (
    Classifier,
    create_inference_session,
    load_labels,
    load_metadata,
)
from app.inference.preprocess import preprocess_image
from tests.conftest import MODEL_DIR

MODEL_PATH = MODEL_DIR / "model.onnx"
TOP_PREDICTION_COUNT = 3


@pytest.mark.skipif(not MODEL_PATH.exists(), reason="model.onnx is not downloaded")
def test_real_model_loads_and_returns_three_tomato_predictions(png_bytes):
    metadata = load_metadata(MODEL_DIR / "metadata.json")
    labels = load_labels(MODEL_DIR / "labels.csv")
    classifier = Classifier(create_inference_session(MODEL_PATH), labels, metadata, Settings())
    tensor = preprocess_image(png_bytes, metadata["input_size"], metadata["mean"], metadata["std"])

    result = classifier.predict(tensor, "tomato")

    assert len(result.top_predictions) == TOP_PREDICTION_COUNT
    assert result.status in {"confident", "unsure"}
    assert all(prediction.label.startswith("tomato_") for prediction in result.top_predictions)
