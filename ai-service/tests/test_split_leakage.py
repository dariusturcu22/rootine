from pathlib import Path

import pandas as pd

SPLITS = Path(__file__).resolve().parents[1] / "training" / "datasets"
MANIFESTS = ["splits_all_38c.csv", "splits_demo1_13c.csv"]


def check_no_leaf_overlap(name):
    df = pd.read_csv(SPLITS / name)
    leaves = df.groupby("split")["leaf_id"].agg(set)
    for a, b in [("train", "val"), ("train", "test"), ("val", "test")]:
        shared = leaves[a] & leaves[b]
        assert not shared, (
            f"{name}: {len(shared)} leaf_id shared by {a} and {b}, e.g. {sorted(shared)[:3]}")
    assert df.groupby("leaf_id")["split"].nunique().max() == 1, (
        f"{name}: a leaf_id spans several splits")
    print(f"OK  {name}: {len(df)} images, {df['leaf_id'].nunique()} leaves, no overlap")


if __name__ == "__main__":
    for m in MANIFESTS:
        check_no_leaf_overlap(m)