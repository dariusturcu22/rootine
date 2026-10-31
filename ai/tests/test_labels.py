from pathlib import Path

from app.inference.classifier import load_crops, load_labels
from app.weather.registry import WEATHER_RULES

MODEL_DIR = Path(__file__).resolve().parent.parent / "models" / "plant-v1"


def test_labels_ids_are_unique():
    labels = load_labels(MODEL_DIR / "labels.csv")

    assert len({label.class_id for label in labels}) == len(labels)
    assert len({label.label for label in labels}) == len(labels)


def test_demo1_ids_are_contiguous_from_zero():
    labels = load_labels(MODEL_DIR / "labels.csv")

    assert sorted(label.demo1_id for label in labels) == list(range(len(labels)))


def test_every_label_crop_is_known():
    labels = load_labels(MODEL_DIR / "labels.csv")
    crops = load_crops(MODEL_DIR / "crops.json")

    assert {label.crop for label in labels} <= set(crops)


def test_weather_rules_are_implemented_names():
    labels = load_labels(MODEL_DIR / "labels.csv")
    crops = load_crops(MODEL_DIR / "crops.json")

    used = {label.weather_rule for label in labels if label.weather_rule}
    used |= {rule for cfg in crops.values() for rule in cfg.weather_rules}
    assert used <= set(WEATHER_RULES)
