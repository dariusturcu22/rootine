from pathlib import Path

from app.inference.classifier import load_crops, load_labels
from app.weather.registry import WEATHER_RULES

MODEL_DIR = Path(__file__).resolve().parent.parent / "models" / "plant-v1"


def test_labels_ids_are_unique_and_contiguous():
    labels = load_labels(MODEL_DIR / "labels.csv")

    class_ids = sorted(label.class_id for label in labels)

    assert len(labels) == 38
    assert class_ids == list(range(38))
    assert len({label.label for label in labels}) == 38


def test_demo1_ids_are_contiguous_from_zero():
    labels = load_labels(MODEL_DIR / "labels.csv")

    demo1_ids = sorted(label.demo1_id for label in labels if label.demo1_id is not None)

    assert len(demo1_ids) == 13
    assert demo1_ids == list(range(13))


def test_demo1_only_contains_tomato_and_potato():
    labels = load_labels(MODEL_DIR / "labels.csv")

    demo1_labels = [label for label in labels if label.demo1_id is not None]

    assert {label.crop for label in demo1_labels} == {
        "tomato",
        "potato",
    }


def test_every_label_crop_is_known():
    labels = load_labels(MODEL_DIR / "labels.csv")
    crops = load_crops(MODEL_DIR / "crops.json")

    assert {label.crop for label in labels} == set(crops)


def test_weather_rules_are_implemented_names():
    labels = load_labels(MODEL_DIR / "labels.csv")
    crops = load_crops(MODEL_DIR / "crops.json")

    used = {
        label.weather_rule
        for label in labels
        if label.weather_rule and label.weather_rule != "none"
    }

    used |= {rule for cfg in crops.values() for rule in cfg.weather_rules}

    assert used <= set(WEATHER_RULES)
