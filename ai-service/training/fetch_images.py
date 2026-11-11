#!/usr/bin/env python3
"""Download the PlantVillage image archive and extract it so the split CSVs resolve.

Run from the ai-service folder:
  python training/fetch_images.py
"""

import argparse
import logging
import zipfile
from pathlib import Path

import pandas as pd
from huggingface_hub import HfApi, hf_hub_download

REPO = "mohanty/PlantVillage"
TRAINING_DIR = Path(__file__).resolve().parent
CSV = TRAINING_DIR / "datasets" / "splits_all_38c.csv"
IMAGES = TRAINING_DIR / "datasets" / "images"

logging.basicConfig(level=logging.INFO, format="%(message)s")
log = logging.getLogger("fetch_images")


def missing_paths(root):
    paths = pd.read_csv(CSV, usecols=["path"])["path"]
    return [p for p in paths if not (root / p).exists()]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--force", action="store_true", help="extract even if images already exist")
    args = parser.parse_args()

    if not CSV.exists():
        raise SystemExit(f"{CSV} not found. Run training/prepare_plantvillage.py first.")

    missing = missing_paths(IMAGES)
    if not missing and not args.force:
        log.info("All images already present under %s", IMAGES)
        return

    sha = HfApi().dataset_info(REPO).sha
    log.info(
        "dataset revision: %s (compare with the revision logged by prepare_plantvillage.py)", sha
    )
    zip_path = hf_hub_download(REPO, "data.zip", repo_type="dataset", revision=sha)
    log.info("Extracting %s to %s (about 2.2 GB, this takes a while)", zip_path, IMAGES)

    IMAGES.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path) as zf:
        zf.extractall(IMAGES)

    missing = missing_paths(IMAGES)
    if missing:
        raise SystemExit(
            f"{len(missing)} images still missing, for example {missing[:3]}. "
            "The archive layout may not match the CSV paths."
        )
    log.info("All %d images present.", len(pd.read_csv(CSV, usecols=["path"])))


if __name__ == "__main__":
    main()
