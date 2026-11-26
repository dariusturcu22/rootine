"""Sanity check and visual grid generator for FieldAugmentor."""

from __future__ import annotations

import argparse
import logging
from pathlib import Path

from PIL import Image, ImageDraw

from training.augmentations import DEFAULT_BG_DIR, IMAGE_EXTENSIONS, FieldAugmentor

logger = logging.getLogger(__name__)


def find_sample_images(images_dir: Path, limit: int = 16) -> list[Path]:
    """Collect up to `limit` image files from images_dir."""
    if not images_dir.exists() or not images_dir.is_dir():
        return []
    paths = [
        p
        for p in sorted(images_dir.rglob("*"))
        if p.is_file() and p.suffix.lower() in IMAGE_EXTENSIONS
    ]
    return paths[:limit]


def make_placeholder_leaf(index: int, size: tuple[int, int] = (224, 224)) -> Image.Image:
    """Fallback synthetic leaf if no dataset images exist yet (e.g. in CI)."""
    w, h = size
    img = Image.new("RGB", (w, h), (185, 185, 185))
    draw = ImageDraw.Draw(img)
    pad_x, pad_y = int(w * 0.2), int(h * 0.15)
    green = (45 + (index * 7) % 35, 120 + (index * 11) % 50, 40)
    draw.ellipse([pad_x, pad_y, w - pad_x, h - pad_y], fill=green, outline=(30, 80, 25), width=2)
    draw.line([(w // 2, pad_y + 6), (w // 2, h - pad_y - 6)], fill=(120, 165, 65), width=2)
    return img


def build_grid(tiles: list[Image.Image], cols: int = 4, rows: int = 4) -> Image.Image:
    """Assemble up to cols*rows RGB tiles into a single contact sheet."""
    tile_w, tile_h = tiles[0].size
    grid = Image.new("RGB", (cols * tile_w, rows * tile_h), (25, 25, 25))
    for idx, tile in enumerate(tiles[: cols * rows]):
        r, c = divmod(idx, cols)
        grid.paste(
            tile.resize((tile_w, tile_h), Image.Resampling.BILINEAR), (c * tile_w, r * tile_h)
        )
    return grid


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Sanity-check FieldAugmentor and write a 4x4 grid."
    )
    parser.add_argument(
        "--images-dir",
        type=Path,
        default=Path("training/datasets/images"),
        help="Directory containing source leaf images.",
    )
    parser.add_argument(
        "--bg-dir",
        type=Path,
        default=DEFAULT_BG_DIR,
        help="Directory containing background images (falls back to synthetic if empty).",
    )
    parser.add_argument(
        "--out",
        type=Path,
        default=Path("training/.artifacts/field_aug_preview.png"),
        help="Output path for the 4x4 preview grid.",
    )
    parser.add_argument("--seed", type=int, default=42, help="Base seed for reproducible check.")
    parser.add_argument(
        "--bg-prob", type=float, default=0.75, help="Background composite probability."
    )
    parser.add_argument(
        "--transform-prob", type=float, default=0.85, help="Field transform probability."
    )
    args = parser.parse_args()

    logging.basicConfig(level=logging.INFO, format="%(message)s")
    augmentor = FieldAugmentor(
        bg_prob=args.bg_prob,
        transform_prob=args.transform_prob,
        bg_dir=args.bg_dir,
        seed=args.seed,
    )
    logger.info("Background images discovered: %d (from %s)", len(augmentor.bg_paths), args.bg_dir)

    sample_paths = find_sample_images(args.images_dir, limit=16)
    sources: list[Image.Image] = []
    for i in range(16):
        if i < len(sample_paths):
            with Image.open(sample_paths[i]) as opened:
                sources.append(opened.convert("RGB").resize((224, 224), Image.Resampling.BILINEAR))
        else:
            sources.append(make_placeholder_leaf(i))

    # Determinism check on item 0 with explicit seed
    out_a = augmentor(sources[0], seed=args.seed)
    out_b = augmentor(sources[0], seed=args.seed)
    assert out_a.size == (224, 224) and out_a.mode == "RGB"
    assert list(out_a.getdata()) == list(out_b.getdata()), (
        "Seeded augmentation was not deterministic"
    )

    augmented_tiles = [augmentor(src, seed=args.seed + idx) for idx, src in enumerate(sources)]
    grid = build_grid(augmented_tiles, cols=4, rows=4)

    args.out.parent.mkdir(parents=True, exist_ok=True)
    grid.save(args.out)
    logger.info("Saved 4x4 preview grid to %s", args.out)


if __name__ == "__main__":
    main()
