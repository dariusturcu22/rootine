#!/usr/bin/env python3
"""Train MobileNetV3-Large on the Tomato & Potato splits.

Trains with class-weighted CrossEntropyLoss, early-stops on validation macro F1,
evaluates the best checkpoint on the test split per class, and exports ONNX
(opset 17, dynamic batch) verified against PyTorch.

Run from the ai-service folder:
  python training/train.py --device cpu --max-per-class 30 --epochs 2    # smoke test
  python training/train.py --epochs 12 --device cuda                     # full run
  python training/train.py --splits training/datasets/splits_all_38c.csv --expected-classes 38
"""

import argparse
import hashlib
import json
import random
import sys
import time
from pathlib import Path

import numpy as np
import onnx
import onnxruntime as ort
import pandas as pd
import torch
import torch.nn as nn
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, f1_score
from torch.utils.data import DataLoader, Dataset
from torchvision import models

# Same preprocessing code path as inference.
AI_SERVICE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(AI_SERVICE_DIR))
from app.inference.preprocess import preprocess_image  # noqa: E402

TRAINING_DIR = Path(__file__).resolve().parent
IMG_SIZE = 224
MEAN = [0.485, 0.456, 0.406]
STD = [0.229, 0.224, 0.225]
SPLIT_ALIASES = {
    "train": "train",
    "training": "train",
    "val": "val",
    "valid": "val",
    "validation": "val",
    "test": "test",
}


def detect_columns(df):
    def pick(candidates, kind):
        for c in candidates:
            if c in df.columns:
                return c
        raise SystemExit(f"Could not find a {kind} column. CSV columns are: {list(df.columns)}")

    path_col = pick(
        ["path", "filepath", "file_path", "image_path", "image", "filename"], "image path"
    )
    split_col = pick(["split", "subset", "set", "partition"], "split")
    if "demo1_id" in df.columns and df["demo1_id"].notna().all():
        label_col = "demo1_id"  # 13-class file: ids match labels.csv
    else:
        label_col = pick(["class_id", "label", "class", "class_name"], "label")  # 38-class file
    return path_col, label_col, split_col


class LeafDataset(Dataset):
    def __init__(self, df, data_root, path_col, augment=False):
        self.paths = [Path(data_root) / p for p in df[path_col]]
        self.targets = df["target"].tolist()
        self.augment = augment

    def __len__(self):
        return len(self.paths)

    def __getitem__(self, i):
        tensor = preprocess_image(self.paths[i].read_bytes(), IMG_SIZE, MEAN, STD)[0]
        if self.augment and random.random() < 0.5:
            # Horizontal flip. Per-channel normalisation commutes with flipping, so this
            # equals flipping before normalisation.
            tensor = tensor[:, :, ::-1].copy()
        return torch.from_numpy(tensor), self.targets[i]


def build_model(num_classes):
    model = models.mobilenet_v3_large(weights=models.MobileNet_V3_Large_Weights.DEFAULT)
    model.classifier[-1] = nn.Linear(model.classifier[-1].in_features, num_classes)
    return model


def run_epoch(model, loader, criterion, optimizer, device, frozen=False):
    training = optimizer is not None
    model.train(training)
    if frozen:
        model.features.eval()  # keep BatchNorm statistics fixed while the backbone is frozen
    total_loss, n, preds, targets = 0.0, 0, [], []
    with torch.enable_grad() if training else torch.no_grad():
        for x, y in loader:
            x, y = x.to(device), y.to(device)
            logits = model(x)
            loss = criterion(logits, y)
            if training:
                optimizer.zero_grad()
                loss.backward()
                optimizer.step()
            total_loss += loss.item() * len(y)
            n += len(y)
            preds.append(logits.argmax(1).cpu())
            targets.append(y.cpu())
    p = torch.cat(preds).numpy()
    t = torch.cat(targets).numpy()
    return total_loss / n, accuracy_score(t, p), f1_score(t, p, average="macro")


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def export_onnx(model, out_path, sample_batch):
    model = model.eval().cpu()
    dummy = torch.randn(1, 3, IMG_SIZE, IMG_SIZE)
    torch.onnx.export(
        model,
        dummy,
        out_path,
        opset_version=17,
        input_names=["input"],
        output_names=["logits"],
        dynamic_axes={"input": {0: "batch"}, "logits": {0: "batch"}},
        dynamo=False,  # keeps dynamic_axes behaviour; needs torch >= 2.5
    )
    onnx.checker.check_model(onnx.load(out_path))

    with torch.no_grad():
        torch_out = model(sample_batch).numpy()
    session = ort.InferenceSession(out_path, providers=["CPUExecutionProvider"])
    onnx_out = session.run(None, {"input": sample_batch.numpy()})[0]
    max_diff = float(np.abs(torch_out - onnx_out).max())
    assert max_diff < 1e-3, f"ONNX/PyTorch max diff {max_diff:.2e} >= 1e-3"
    return max_diff


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--splits", default=str(TRAINING_DIR / "datasets" / "splits_demo1_13c.csv"))
    parser.add_argument(
        "--data-root",
        default=str(TRAINING_DIR / "datasets" / "images"),
        help="Folder that the CSV's image paths are relative to",
    )
    parser.add_argument(
        "--output-dir", default=str(AI_SERVICE_DIR / "models" / "plant-v1" / "outputs")
    )
    parser.add_argument("--expected-classes", type=int, default=13)
    parser.add_argument("--epochs", type=int, default=12)
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--patience", type=int, default=4, help="early stopping on val macro F1")
    parser.add_argument(
        "--freeze-epochs", type=int, default=2, help="train only the head for N epochs"
    )
    parser.add_argument("--num-workers", type=int, default=2)
    parser.add_argument("--max-per-class", type=int, default=0, help="0 = use all (smoke tests)")
    parser.add_argument("--device", default="auto", choices=["auto", "cpu", "cuda"])
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    random.seed(args.seed)
    np.random.seed(args.seed)
    torch.manual_seed(args.seed)
    device = torch.device(
        ("cuda" if torch.cuda.is_available() else "cpu") if args.device == "auto" else args.device
    )
    out = Path(args.output_dir)
    out.mkdir(parents=True, exist_ok=True)

    # ---- Data ----
    df = pd.read_csv(args.splits)
    path_col, label_col, split_col = detect_columns(df)
    df[split_col] = df[split_col].astype(str).str.strip().str.lower().map(SPLIT_ALIASES)
    if df[split_col].isna().any():
        raise SystemExit("Unknown values in the split column. Expected train/val/test.")
    df[label_col] = df[label_col].astype(int)

    if args.max_per_class > 0:
        df = df.groupby([split_col, label_col], group_keys=False).head(args.max_per_class)

    missing = [p for p in df[path_col] if not (Path(args.data_root) / p).exists()]
    if missing:
        raise SystemExit(
            f"{len(missing)} image files not found under {args.data_root}. "
            f"First few: {missing[:5]}. Run training/fetch_images.py first, or pass --data-root."
        )

    # Model output index i corresponds to label_ids[i]; classes.json records the mapping.
    label_ids = sorted(df[label_col].unique())
    if len(label_ids) != args.expected_classes:
        raise SystemExit(f"Expected {args.expected_classes} classes, found {len(label_ids)}.")
    if {"crop", "disease"}.issubset(df.columns):
        name_of = {
            k: f"{g['crop'].iloc[0]}___{g['disease'].iloc[0]}" for k, g in df.groupby(label_col)
        }
    else:
        name_of = {k: str(k) for k in label_ids}
    classes = [name_of[k] for k in label_ids]
    index_of = {k: i for i, k in enumerate(label_ids)}
    df["target"] = df[label_col].map(index_of)

    class_map = [
        {"index": i, "label_id": int(k), "name": classes[i]} for i, k in enumerate(label_ids)
    ]
    (out / "classes.json").write_text(json.dumps(class_map, indent=2))

    splits = {s: df[df[split_col] == s] for s in ["train", "val", "test"]}
    for name, part in splits.items():
        if part.empty:
            raise SystemExit(f"The '{name}' split is empty.")

    loaders = {
        "train": DataLoader(
            LeafDataset(splits["train"], args.data_root, path_col, augment=True),
            batch_size=args.batch_size,
            shuffle=True,
            num_workers=args.num_workers,
        ),
        "val": DataLoader(
            LeafDataset(splits["val"], args.data_root, path_col),
            batch_size=args.batch_size,
            num_workers=args.num_workers,
        ),
        "test": DataLoader(
            LeafDataset(splits["test"], args.data_root, path_col),
            batch_size=args.batch_size,
            num_workers=args.num_workers,
        ),
    }

    # Class weights: inverse frequency on the train split, normalised to mean 1.
    counts = np.bincount(splits["train"]["target"].to_numpy(), minlength=len(classes)).astype(
        np.float32
    )
    if (counts == 0).any():
        raise SystemExit(
            f"Classes with no training images: {[classes[i] for i in np.where(counts == 0)[0]]}"
        )
    weights = counts.sum() / (len(classes) * counts)
    weights = torch.tensor(weights / weights.mean(), device=device)
    criterion = nn.CrossEntropyLoss(weight=weights)

    # ---- Model ----
    model = build_model(len(classes)).to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr)
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=args.epochs)

    best_f1, best_epoch, history = -1.0, -1, []
    for epoch in range(args.epochs):
        frozen = epoch < args.freeze_epochs
        for p in model.features.parameters():
            p.requires_grad = not frozen

        t0 = time.time()
        tr_loss, tr_acc, tr_f1 = run_epoch(
            model, loaders["train"], criterion, optimizer, device, frozen
        )
        epoch_time = time.time() - t0
        va_loss, va_acc, va_f1 = run_epoch(model, loaders["val"], criterion, None, device)
        scheduler.step()

        row = dict(
            epoch=epoch + 1,
            frozen_backbone=frozen,
            epoch_seconds=round(epoch_time, 1),
            train_loss=tr_loss,
            train_acc=float(tr_acc),
            train_macro_f1=float(tr_f1),
            val_loss=va_loss,
            val_acc=float(va_acc),
            val_macro_f1=float(va_f1),
        )
        history.append(row)
        print(json.dumps(row), flush=True)

        if va_f1 > best_f1:
            best_f1, best_epoch = va_f1, epoch + 1
            torch.save(
                {"state_dict": model.state_dict(), "classes": classes, "img_size": IMG_SIZE},
                out / "best.pt",
            )
        elif epoch + 1 - best_epoch >= args.patience:
            print(f"Early stopping at epoch {epoch + 1} (best epoch {best_epoch})")
            break

    (out / "history.json").write_text(json.dumps(history, indent=2))

    # ---- Test evaluation with the best checkpoint ----
    ckpt = torch.load(out / "best.pt", map_location=device, weights_only=True)
    model.load_state_dict(ckpt["state_dict"])
    model.eval()
    preds, targets = [], []
    with torch.no_grad():
        for x, y in loaders["test"]:
            preds.append(model(x.to(device)).argmax(1).cpu())
            targets.append(y)
    p = torch.cat(preds).numpy()
    t = torch.cat(targets).numpy()

    labels = list(range(len(classes)))
    report = classification_report(
        t, p, labels=labels, target_names=classes, output_dict=True, zero_division=0
    )
    pd.DataFrame(report).T.to_csv(out / "per_class_metrics.csv")

    lines = ["| Class | Precision | Recall | F1 | Support |", "|---|---|---|---|---|"]
    for c in classes:
        r = report[c]
        lines.append(
            f"| {c} | {r['precision']:.3f} | {r['recall']:.3f} | "
            f"{r['f1-score']:.3f} | {int(r['support'])} |"
        )
    (out / "per_class_metrics.md").write_text("\n".join(lines) + "\n")

    cm = confusion_matrix(t, p, labels=labels)
    pd.DataFrame(cm, index=classes, columns=classes).to_csv(out / "confusion_matrix.csv")

    metrics = {
        "best_epoch": best_epoch,
        "test_accuracy": float(accuracy_score(t, p)),
        "test_macro_f1": float(f1_score(t, p, average="macro")),
        "num_classes": len(classes),
        "num_test_images": int(len(t)),
        "device": str(device),
        "total_train_seconds": round(sum(r["epoch_seconds"] for r in history), 1),
    }
    (out / "metrics.json").write_text(json.dumps(metrics, indent=2))
    print(json.dumps(metrics, indent=2))

    onnx_path = out / "model.onnx"
    sample_x, _ = next(iter(loaders["test"]))
    max_diff = export_onnx(model, str(onnx_path), sample_x[:8])
    check = {
        "onnx_file": str(onnx_path),
        "onnx_sha256": sha256(onnx_path),
        "onnx_max_diff_vs_pytorch": max_diff,
        "input_shape": ["batch", 3, IMG_SIZE, IMG_SIZE],
        "output_shape": ["batch", len(classes)],
        "opset": 17,
        "classes": classes,
    }
    (out / "onnx_check.json").write_text(json.dumps(check, indent=2))
    print(f"ONNX max abs diff vs PyTorch: {max_diff:.2e}")
    print(f"Done. Outputs in {out}")


if __name__ == "__main__":
    main()
