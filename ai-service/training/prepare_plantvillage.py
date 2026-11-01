import io
import json
import logging
import re
import zipfile
from pathlib import Path

import pandas as pd
from sklearn.model_selection import GroupShuffleSplit

REPO = "mohanty/PlantVillage"
REVISION = None
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "training" / "datasets"
LABELS = ROOT / "models" / "plant-v1" / "labels.csv"
SEED = 42
MAX_HAMMING = 3
logging.basicConfig(level=logging.INFO, format="%(message)s")
log = logging.getLogger("prepare_plantvillage")


def stem(path):
    """'raw/color/X/<uuid>___RUTG._HL 3733.JPG' -> 'rutg._hl 3733' (leaf-map.json key format)."""
    name = path.strip().replace("\\", "/").split("/")[-1].split("___")[-1].lower()
    return re.sub(r"\.(jpe?g|png)$", "", name)


def build_table(file_lists, leaf_map):
    paths = sorted({p.strip() for p in file_lists if p.strip()})
    df = pd.DataFrame({"path": paths})
    df["class_name"] = df["path"].map(lambda p: p.replace("\\", "/").split("/")[-2])

    def leaf_of(path, cls):
        ids = leaf_map.get(stem(path), [])
        ids = [ids] if isinstance(ids, str) else ids
        hit = [i for i in ids if i.startswith(cls + ":::")]
        return hit[0] if hit else None

    df["leaf_id"] = [leaf_of(p, c) for p, c in zip(df["path"], df["class_name"], strict=True)]
    mapped = df["leaf_id"].notna().mean()
    log.info(
        "files mapped by leaf-map.json: %d / %d (%.0f%%)",
        df["leaf_id"].notna().sum(),
        len(df),
        100 * mapped,
    )
    assert mapped > 0.5, (
        "leaf-map.json matches too few files; "
        f"sample: {next(iter(leaf_map.items()))!r} / {paths[0]}"
    )
    names = sorted(df["class_name"].unique())  # class_id = position in the sorted class list
    df["class_id"] = df["class_name"].map({n: i for i, n in enumerate(names)})
    return df, names


def dhash_groups(df, zip_path):
    """Fill leaf_id for unmapped files: merge same-class images with near-identical dhash."""
    import imagehash
    from PIL import Image

    todo = list(df.index[df["leaf_id"].isna()])
    if not todo:
        return
    hashes = {}
    with zipfile.ZipFile(zip_path) as zf:
        names = set(zf.namelist())

        def member(i):
            return df.at[i, "path"]  # zip keeps the raw/color/... prefix

        assert member(todo[0]) in names, (
            f"{member(todo[0])!r} not in data.zip, e.g. {sorted(names)[:3]}"
        )
        for n, i in enumerate(todo):
            img = Image.open(io.BytesIO(zf.read(member(i)))).convert("L")
            hashes[i] = int(str(imagehash.dhash(img)), 16)
            if n % 2000 == 0:
                log.info("dhash %d / %d", n, len(todo))

    parent = {i: i for i in todo}

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    for band in range(4):  # 4 x 16-bit bands: any pair within 3 bits shares an identical band
        buckets = {}
        for i, h in hashes.items():
            key = (df.at[i, "class_name"], (h >> (16 * band)) & 0xFFFF)
            buckets.setdefault(key, []).append(i)
        for m in buckets.values():
            for a in range(len(m)):
                for b in range(a + 1, len(m)):
                    if bin(hashes[m[a]] ^ hashes[m[b]]).count("1") <= MAX_HAMMING:
                        parent[find(m[b])] = find(m[a])

    df.loc[todo, "leaf_id"] = [f"{df.at[i, 'class_name']}:::dh_{find(i)}" for i in todo]
    log.info("unmapped files: %d -> %d dhash groups", len(todo), len({find(i) for i in todo}))


def check_labels(labels, names):
    """labels.csv lists only the Demo 1 classes. Their class_ids must be valid dataset ids
    and crop/disease must match the dataset class name (compared loosely)."""
    bad_ids = set(labels["class_id"]) - set(range(len(names)))
    assert not bad_ids, (
        f"labels.csv class_ids not in 0..{len(names) - 1}: {sorted(bad_ids)}\n"
        "dataset classes (id: name):\n" + "\n".join(f"{i}: {n}" for i, n in enumerate(names))
    )

    def norm(s):
        return re.sub(r"[^a-z0-9]", "", str(s).lower())

    for _, r in labels.iterrows():
        n = norm(names[r["class_id"]])
        assert norm(r["crop"]) in n and norm(r["disease"]) in n, (
            f"labels.csv class {r['class_id']} is {r['crop']}/{r['disease']} "
            f"but the dataset class with that id is {names[r['class_id']]!r}; "
            "class_ids are alphabetical positions of the folder names"
        )


def log_distribution(df, title):
    t = pd.crosstab(df["class_id"], df["split"])[["train", "val", "test"]]
    t["total"] = t.sum(axis=1)
    log.info("\n%s (%d classes, %d images)\n%s", title, len(t), len(df), t.to_string())


def main():
    from huggingface_hub import HfApi, hf_hub_download

    OUT.mkdir(parents=True, exist_ok=True)
    sha = HfApi().dataset_info(REPO, revision=REVISION).sha
    log.info("dataset revision: %s", sha)

    def get(name):
        return hf_hub_download(REPO, name, repo_type="dataset", revision=sha)

    lines = []
    for f in ("splits/color_train.txt", "splits/color_test.txt"):
        with open(get(f), encoding="utf-8") as fh:
            lines += fh.read().splitlines()
    with open(get("leaf_grouping/leaf-map.json"), encoding="utf-8") as fh:
        leaf_map = json.load(fh)

    df, names = build_table(lines, leaf_map)
    labels = pd.read_csv(LABELS)
    check_labels(labels, names)  # fail early, before the slow dhash step
    if df["leaf_id"].isna().any():
        dhash_groups(df, get("data.zip"))  # 2.2 GB, downloaded once and cached
    log.info("images: %d, classes: %d, leaves: %d", len(df), len(names), df["leaf_id"].nunique())

    df[["crop", "disease"]] = df["class_name"].str.split("___", n=1, expand=True)
    # only Demo 1 classes have a demo1_id
    df = df.merge(labels[["class_id", "demo1_id"]], on="class_id", how="left")

    # 70 / 15 / 15, grouped by leaf
    gss = GroupShuffleSplit(1, test_size=0.30, random_state=SEED)
    tr, rest = next(gss.split(df, groups=df["leaf_id"]))
    train, rest = df.iloc[tr], df.iloc[rest]
    gss = GroupShuffleSplit(1, test_size=0.50, random_state=SEED)
    va, te = next(gss.split(rest, groups=rest["leaf_id"]))
    df["split"] = ""
    df.loc[train.index, "split"] = "train"
    df.loc[rest.index[va], "split"] = "val"
    df.loc[rest.index[te], "split"] = "test"

    # leakage guard (also covered by tests/test_split_leakage.py)
    leaves = df.groupby("split")["leaf_id"].agg(set)
    assert not (
        leaves["train"] & leaves["val"]
        or leaves["train"] & leaves["test"]
        or leaves["val"] & leaves["test"]
    )

    cols = ["path", "leaf_id", "crop", "disease", "class_id", "demo1_id", "split"]
    demo = df[df["demo1_id"].notnull()]
    df[cols].to_csv(OUT / "splits_all_38c.csv", index=False)
    demo[cols].to_csv(OUT / "splits_demo1_13c.csv", index=False)

    log_distribution(df, "all")
    log_distribution(demo, "demo1")


if __name__ == "__main__":
    main()