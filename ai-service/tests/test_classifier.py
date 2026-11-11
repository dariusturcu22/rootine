import numpy as np
import pytest

from app.config import Settings
from app.inference.classifier import Classifier, load_labels, load_metadata, softmax
from tests.conftest import MODEL_DIR
from tests.fakes import (
    DEMO_CLASS_COUNT,
    POTATO_EARLY_BLIGHT,
    POTATO_LATE_BLIGHT,
    TOMATO_EARLY_BLIGHT,
    FakeSession,
    logits_favouring,
)

LABELS = load_labels(MODEL_DIR / "labels.csv")
METADATA = load_metadata(MODEL_DIR / "metadata.json")
INPUT_SIZE = METADATA["input_size"]
TOP_PREDICTION_COUNT = 3
POTATO_CLASS_COUNT = 3
LOW_CONFIDENCE_THRESHOLD = 0.2


def build_classifier(logits: list[float], metadata: dict | None = None) -> Classifier:
    return Classifier(FakeSession(logits), LABELS, metadata or METADATA, Settings())


def blank_tensor() -> np.ndarray:
    return np.zeros((1, 3, INPUT_SIZE, INPUT_SIZE), dtype=np.float32)


def test_softmax_probabilities_sum_to_one():
    probabilities = softmax(np.asarray([1000.0, 999.0, -5.0]))

    assert probabilities.sum() == pytest.approx(1.0)
    assert np.all(np.isfinite(probabilities))


def test_classifier_reads_input_name_from_session():
    session = FakeSession(logits_favouring(TOMATO_EARLY_BLIGHT))
    classifier = Classifier(session, LABELS, METADATA, Settings())

    classifier.predict(blank_tensor(), "tomato")

    assert set(session.received_input_names) == {FakeSession.input_name}


def test_tomato_mask_keeps_only_tomato_classes_in_descending_order():
    classifier = build_classifier(logits_favouring(TOMATO_EARLY_BLIGHT))

    result = classifier.predict(blank_tensor(), "tomato")

    probabilities = [prediction.probability for prediction in result.top_predictions]
    assert len(result.top_predictions) == TOP_PREDICTION_COUNT
    assert all(prediction.label.startswith("tomato_") for prediction in result.top_predictions)
    assert result.top_predictions[0].label == "tomato_early_blight"
    assert probabilities == sorted(probabilities, reverse=True)
    assert result.status == "confident"
    assert result.crop_check == "ok"


def test_potato_mask_renormalizes_over_potato_classes():
    classifier = build_classifier(logits_favouring(POTATO_LATE_BLIGHT))

    result = classifier.predict(blank_tensor(), "potato")

    total_probability = sum(prediction.probability for prediction in result.top_predictions)
    assert len(result.top_predictions) == POTATO_CLASS_COUNT
    assert total_probability == pytest.approx(1.0)
    assert result.top_predictions[0].label == "potato_late_blight"
    assert result.crop_check == "ok"


def test_tomato_photo_sent_as_potato_is_a_mismatch_and_unsure():
    classifier = build_classifier(logits_favouring(TOMATO_EARLY_BLIGHT))

    result = classifier.predict(blank_tensor(), "potato")

    assert result.crop == "potato"
    assert result.crop_check == "mismatch"
    assert result.status == "unsure"
    assert all(prediction.label.startswith("potato_") for prediction in result.top_predictions)


def test_flat_logits_are_unsure_below_the_default_threshold():
    classifier = build_classifier([0.0] * DEMO_CLASS_COUNT)

    result = classifier.predict(blank_tensor(), "tomato")

    assert result.status == "unsure"


def test_confidence_threshold_from_metadata_overrides_the_default():
    metadata = {**METADATA, "confidence_threshold": LOW_CONFIDENCE_THRESHOLD}
    classifier = build_classifier([0.0] * DEMO_CLASS_COUNT, metadata)

    result = classifier.predict(blank_tensor(), "potato")

    assert result.status == "confident"


def test_without_crop_uses_the_crop_of_the_best_class():
    classifier = build_classifier(logits_favouring(POTATO_EARLY_BLIGHT))

    result = classifier.predict(blank_tensor(), None)

    assert result.crop == "potato"
    assert result.crop_check == "ok"
    assert result.top_predictions[0].label == "potato_early_blight"
    assert len(result.top_predictions) == TOP_PREDICTION_COUNT


def test_model_with_wrong_number_of_classes_is_refused():
    with pytest.raises(RuntimeError, match="classes"):
        build_classifier([0.0] * (DEMO_CLASS_COUNT - 1))
