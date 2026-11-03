import importlib.util
from pathlib import Path

import pandas as pd
import pytest

SCRIPT = Path(__file__).resolve().parents[1] / "training" / "prepare_plantvillage.py"
spec = importlib.util.spec_from_file_location("prepare_plantvillage", SCRIPT)
prep = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prep)

NAMES = ["Apple___scab", "Tomato___Late_blight", "Tomato___healthy"]


def test_stem_matches_leaf_map_keys():
    path = "raw/color/Corn_(maize)___Common_rust_/00ab-12___RUTG._HL 3733.JPG"
    assert prep.stem(path) == "rutg._hl 3733"


def test_build_table_picks_leaf_of_the_files_class():
    # the same stem exists in two classes; each file must get its own class's leaf
    files = [
        "raw/color/Apple___scab/u1___RS_HL 1.JPG",
        "raw/color/Tomato___healthy/u2___RS_HL 1.JPG",
        "raw/color/Tomato___healthy/u3___RS_HL 2.JPG",
    ]
    leaf_map = {
        "rs_hl 1": ["Apple___scab:::7.0", "Tomato___healthy:::3.0"],
        "rs_hl 2": ["Tomato___healthy:::4.0"],
    }
    df, names = prep.build_table(files, leaf_map)
    assert names == ["Apple___scab", "Tomato___healthy"]
    assert df["leaf_id"].tolist() == [
        "Apple___scab:::7.0",
        "Tomato___healthy:::3.0",
        "Tomato___healthy:::4.0",
    ]
    assert df["class_id"].tolist() == [0, 1, 1]


def test_build_table_leaves_unmapped_files_empty():
    files = [f"raw/color/Apple___scab/u___A {i}.JPG" for i in range(4)]
    leaf_map = {f"a {i}": [f"Apple___scab:::{i}.0"] for i in range(3)}
    df, _ = prep.build_table(files, leaf_map)
    assert df["leaf_id"].isna().tolist() == [False, False, False, True]


def test_build_table_fails_when_map_does_not_match():
    files = [f"raw/color/Apple___scab/u___A {i}.JPG" for i in range(4)]
    with pytest.raises(AssertionError, match="too few files"):
        prep.build_table(files, {"other 1": ["Apple___scab:::1.0"]})


def labels_frame(class_id, crop, disease):
    return pd.DataFrame({"class_id": [class_id], "crop": [crop], "disease": [disease]})


def test_check_labels_accepts_matching_class():
    prep.check_labels(labels_frame(1, "tomato", "late_blight"), NAMES)


def test_check_labels_rejects_wrong_disease():
    with pytest.raises(AssertionError, match="labels.csv class 1"):
        prep.check_labels(labels_frame(1, "tomato", "early_blight"), NAMES)


def test_check_labels_rejects_unknown_class_id():
    with pytest.raises(AssertionError, match="not in 0..2"):
        prep.check_labels(labels_frame(9, "tomato", "healthy"), NAMES)


def leaf_frame(n_leaves=200, images_per_leaf=3):
    leaves = [f"leaf{i}" for i in range(n_leaves) for _ in range(images_per_leaf)]
    return pd.DataFrame({"leaf_id": leaves})


def test_split_keeps_each_leaf_in_one_split():
    df = leaf_frame()
    df["split"] = prep.split_by_leaf(df)
    assert set(df["split"]) == {"train", "val", "test"}
    assert df.groupby("leaf_id")["split"].nunique().max() == 1
    prep.assert_no_leaf_overlap(df)


def test_split_is_roughly_70_15_15_and_reproducible():
    df = leaf_frame()
    first = prep.split_by_leaf(df)
    assert first.equals(prep.split_by_leaf(df))
    share = first.value_counts(normalize=True)
    assert 0.65 < share["train"] < 0.75
    assert 0.10 < share["val"] < 0.20
    assert 0.10 < share["test"] < 0.20


def test_overlap_check_catches_a_leaked_leaf():
    df = pd.DataFrame({"leaf_id": ["a", "a", "b", "c"], "split": ["train", "val", "val", "test"]})
    with pytest.raises(AssertionError, match="shared by train and val"):
        prep.assert_no_leaf_overlap(df)


def test_distribution_table_counts_images_per_class_and_split():
    df = pd.DataFrame(
        {
            "class_name": ["A___x", "A___x", "A___x", "B___y", "B___y"],
            "class_id": [0, 0, 0, 1, 1],
            "split": ["train", "train", "val", "train", "test"],
        }
    )
    report = prep.distribution_table(df, "all")
    assert report.splitlines()[0] == "all: 2 classes, 5 images (train 3, val 1, test 1)"
    assert "A___x" in report and "B___y" in report
