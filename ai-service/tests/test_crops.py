import csv
import json
from collections import defaultdict
from pathlib import Path

LABELS_PATH = Path(__file__).parent.parent / "models" / "plant-v1" / "labels.csv"
CROPS_PATH = Path(__file__).parent.parent / "models" / "plant-v1" / "crops.json"


def _load_crops_config():
    with open(CROPS_PATH) as f:
        return json.load(f)["crops"]


def _load_labels_rows():
    with open(LABELS_PATH) as f:
        return list(csv.DictReader(f))


def test_crops_json_weather_rules_match_labels_csv():
    """crops.json weather_rules for each crop must match the distinct,
    non-'none' weather_rule values for that crop in labels.csv."""
    rows = _load_labels_rows()
    crops_config = _load_crops_config()

    rules_by_crop = defaultdict(set)
    for row in rows:
        rule = row["weather_rule"]
        if rule and rule != "none":
            rules_by_crop[row["crop"]].add(rule)

    for crop, rules in rules_by_crop.items():
        configured = set(crops_config[crop]["weather_rules"])
        assert configured == rules, (
            f"{crop}: crops.json weather_rules={configured} "
            f"!= labels.csv weather_rule values={rules}"
        )

    for crop, config in crops_config.items():
        if crop not in rules_by_crop:
            assert config["weather_rules"] == [], (
                f"{crop}: has weather_rules in crops.json but no weather_rule in labels.csv"
            )


def test_unused_weather_rules_are_not_referenced():
    """gubler_thomas does not appear don't appear in labels.csv, so no
    crop in crops.json should reference them."""
    rows = _load_labels_rows()
    rules_in_csv = {row["weather_rule"] for row in rows} - {"none", ""}
    assert rules_in_csv == {"hutton", "mills", "spotts", "tomcast"}

    crops_config = _load_crops_config()
    unused_rules = {"gubler_thomas"}
    for crop, config in crops_config.items():
        assert unused_rules.isdisjoint(config["weather_rules"]), (
            f"{crop} references a weather rule not present in labels.csv"
        )


def test_multiclass_crops_in_labels_csv_are_enabled():
    """All 9 crops with >1 class in labels.csv should be enabled +
    diagnosis_supported in crops.json; single-class crops should not."""
    rows = _load_labels_rows()
    crops_config = _load_crops_config()

    classes_by_crop = defaultdict(set)
    for row in rows:
        classes_by_crop[row["crop"]].add(row["disease"])

    multi_class_crops = {c for c, d in classes_by_crop.items() if len(d) > 1}
    single_class_crops = set(classes_by_crop) - multi_class_crops

    assert multi_class_crops == {
        "apple",
        "cherry",
        "corn",
        "grape",
        "peach",
        "pepper",
        "potato",
        "strawberry",
        "tomato",
    }
    assert single_class_crops == {
        "blueberry",
        "orange",
        "raspberry",
        "soybean",
        "squash",
    }

    for crop in multi_class_crops:
        assert crops_config[crop]["enabled"] is True
        assert crops_config[crop]["diagnosis_supported"] is True
        assert crops_config[crop]["support_level"] == "lab_only"

    for crop in single_class_crops:
        assert crops_config[crop]["enabled"] is False
        assert crops_config[crop]["diagnosis_supported"] is False
        assert crops_config[crop]["support_level"] == "not_supported"
