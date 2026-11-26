import io
import random
from functools import lru_cache
from pathlib import Path

import numpy as np
from PIL import Image, ImageEnhance, ImageFilter, ImageOps

OUTDOOR_BACKGROUNDS = ("soil", "foliage", "sky", "wood_field")
IMAGE_EXTENSIONS = (".jpg", ".jpeg", ".png", ".webp", ".bmp")
DEFAULT_BG_DIR = Path(__file__).resolve().parent / "backgrounds"


def make_rng(seed: int | None = None) -> tuple[random.Random, np.random.Generator]:
    """Create paired Python and NumPy RNGs from an optional integer seed."""
    if seed is None:
        py_rng = random.Random()
        np_seed = int(py_rng.randrange(0, 2**32))
    else:
        py_rng = random.Random(seed)
        np_seed = int(seed) & 0xFFFFFFFF
    return py_rng, np.random.default_rng(np_seed)


@lru_cache(maxsize=64)
def _norm_grid(width: int, height: int) -> tuple[np.ndarray, np.ndarray]:
    """Cached normalized (nx, ny) coordinate grids in [0.0, 1.0] for (height, width)."""
    yy, xx = np.mgrid[0:height, 0:width].astype(np.float32)
    nx = xx / max(width - 1, 1)
    ny = yy / max(height - 1, 1)
    return nx, ny


@lru_cache(maxsize=512)
def _cached_bg_image(path_str: str) -> Image.Image:
    """Cache decoded RGB background images per worker process to avoid repeated disk I/O."""
    with Image.open(path_str) as opened:
        return ImageOps.exif_transpose(opened).convert("RGB")


def discover_background_images(bg_dir: str | Path | None = DEFAULT_BG_DIR) -> list[Path]:
    """Recursively discover local background images under bg_dir."""
    if bg_dir is None:
        return []
    root = Path(bg_dir)
    if not root.exists() or not root.is_dir():
        return []
    return [
        p for p in sorted(root.rglob("*")) if p.is_file() and p.suffix.lower() in IMAGE_EXTENSIONS
    ]


def generate_outdoor_background(
    width: int,
    height: int,
    np_rng: np.random.Generator,
    py_rng: random.Random,
    bg_type: str | None = None,
) -> Image.Image:
    """Generate a fallback outdoor background (soil, foliage, sky, or wood/field)."""
    nx, ny = _norm_grid(width, height)

    coarse = np_rng.normal(0.0, 1.0, size=(max(4, height // 16), max(4, width // 16))).astype(
        np.float32
    )
    span = float(np.ptp(coarse))
    coarse_norm = (coarse - float(coarse.min())) / (span + 1e-6)
    blob = (
        np.asarray(
            Image.fromarray(np.clip(coarse_norm * 255.0, 0, 255).astype(np.uint8)).resize(
                (width, height), Image.Resampling.BILINEAR
            ),
            dtype=np.float32,
        )
        / 255.0
    )
    fine = np_rng.normal(0.0, 0.08, size=(height, width)).astype(np.float32)

    chosen = bg_type if bg_type in OUTDOOR_BACKGROUNDS else py_rng.choice(OUTDOOR_BACKGROUNDS)

    if chosen == "soil":
        base = np.stack(
            [
                68.0 + 45.0 * blob + 22.0 * fine,
                48.0 + 34.0 * blob + 16.0 * fine,
                32.0 + 22.0 * blob + 12.0 * fine,
            ],
            axis=-1,
        )
    elif chosen == "foliage":
        streaks = 0.5 + 0.5 * np.sin((nx * 8.0 + ny * 3.0 + blob * 2.5) * np.pi)
        base = np.stack(
            [
                32.0 + 42.0 * blob + 25.0 * streaks + 18.0 * fine,
                72.0 + 68.0 * blob + 35.0 * streaks + 20.0 * fine,
                28.0 + 24.0 * blob + 12.0 * streaks + 12.0 * fine,
            ],
            axis=-1,
        )
    elif chosen == "sky":
        horizon = np.clip(ny**1.5, 0.0, 1.0)
        clouds = np.clip((blob - 0.55) * 2.2, 0.0, 1.0)
        base = np.stack(
            [
                95.0 * (1.0 - horizon) + 195.0 * horizon + 55.0 * clouds + 8.0 * fine,
                155.0 * (1.0 - horizon) + 215.0 * horizon + 55.0 * clouds + 8.0 * fine,
                225.0 * (1.0 - horizon) + 230.0 * horizon + 45.0 * clouds + 8.0 * fine,
            ],
            axis=-1,
        )
    else:  # "wood_field"
        grain = 0.5 + 0.5 * np.sin((ny * 18.0 + blob * 4.0) * np.pi)
        base = np.stack(
            [
                118.0 + 55.0 * grain + 25.0 * blob + 16.0 * fine,
                88.0 + 42.0 * grain + 20.0 * blob + 14.0 * fine,
                52.0 + 24.0 * grain + 12.0 * blob + 10.0 * fine,
            ],
            axis=-1,
        )

    return Image.fromarray(np.clip(base, 0, 255).astype(np.uint8), mode="RGB")


def load_background_crop(
    width: int,
    height: int,
    bg_paths: list[Path],
    np_rng: np.random.Generator,
    py_rng: random.Random,
) -> Image.Image:
    """Load an aspect-safe crop from a real background image, or fallback to synthetic."""
    if not bg_paths:
        return generate_outdoor_background(width, height, np_rng, py_rng)

    chosen_path = py_rng.choice(bg_paths)
    bg_img = _cached_bg_image(str(chosen_path))

    bg_w, bg_h = bg_img.size
    target_ratio = width / max(height, 1)
    bg_ratio = bg_w / max(bg_h, 1)

    if bg_ratio > target_ratio:
        crop_w = max(1, int(round(bg_h * target_ratio)))
        crop_h = bg_h
        left = int(py_rng.randint(0, max(0, bg_w - crop_w)))
        top = 0
    else:
        crop_w = bg_w
        crop_h = max(1, int(round(bg_w / max(target_ratio, 1e-6))))
        left = 0
        top = int(py_rng.randint(0, max(0, bg_h - crop_h)))

    cropped = bg_img.crop((left, top, left + crop_w, top + crop_h))
    return cropped.resize((width, height), Image.Resampling.BILINEAR)


def segment_leaf_mask(rgb: np.ndarray) -> np.ndarray:
    """Segment leaf foreground into a soft mask in [0.0, 1.0] for (H, W, 3) uint8 RGB."""
    h, w, _ = rgb.shape
    img_f = rgb.astype(np.float32)

    border_samples = np.concatenate(
        [img_f[0, :, :], img_f[-1, :, :], img_f[:, 0, :], img_f[:, -1, :]],
        axis=0,
    )
    bg_color = np.median(border_samples, axis=0)
    color_dist = np.linalg.norm(img_f - bg_color[None, None, :], axis=-1) / 255.0

    r, g, b = img_f[..., 0] / 255.0, img_f[..., 1] / 255.0, img_f[..., 2] / 255.0
    excess_green = np.clip((2.0 * g - r - b + 0.25) * 0.8, 0.0, 1.5)
    chroma = np.maximum(np.maximum(r, g), b) - np.minimum(np.minimum(r, g), b)

    score = (
        0.55 * np.clip(color_dist * 2.2, 0.0, 1.0)
        + 0.30 * np.clip(excess_green, 0.0, 1.0)
        + 0.15 * np.clip(chroma * 2.0, 0.0, 1.0)
    )
    fg_bool = score > 0.36

    mask_img = Image.fromarray((fg_bool.astype(np.uint8) * 255), mode="L")
    mask_img = mask_img.filter(ImageFilter.MaxFilter(size=5))
    mask_img = mask_img.filter(ImageFilter.MinFilter(size=5))
    mask_img = mask_img.filter(ImageFilter.GaussianBlur(radius=2.0))
    soft_mask = np.asarray(mask_img, dtype=np.float32) / 255.0

    fg_ratio = float((soft_mask > 0.5).mean())
    if fg_ratio < 0.08 or fg_ratio > 0.92:
        nx, ny = _norm_grid(w, h)
        ry, rx = 0.38, 0.38
        ell = (((nx - 0.5) / rx) ** 2 + ((ny - 0.5) / ry) ** 2) <= 1.0
        ell_img = Image.fromarray((ell.astype(np.uint8) * 255), mode="L").filter(
            ImageFilter.GaussianBlur(radius=4.0)
        )
        soft_mask = np.asarray(ell_img, dtype=np.float32) / 255.0

    return np.clip(soft_mask, 0.0, 1.0)


def composite_outdoor_background(
    image: Image.Image,
    np_rng: np.random.Generator,
    py_rng: random.Random,
    bg_paths: list[Path] | None = None,
) -> Image.Image:
    """Segment leaf foreground from uniform background and composite onto outdoor scene."""
    rgb_img = image.convert("RGB")
    w, h = rgb_img.size
    rgb_arr = np.asarray(rgb_img, dtype=np.uint8)
    mask = segment_leaf_mask(rgb_arr)[..., None]

    bg_img = load_background_crop(w, h, bg_paths or [], np_rng, py_rng)
    bg_arr = np.asarray(bg_img, dtype=np.float32)
    fg_arr = rgb_arr.astype(np.float32)

    composited = fg_arr * mask + bg_arr * (1.0 - mask)
    return Image.fromarray(np.clip(composited, 0, 255).astype(np.uint8), mode="RGB")


def apply_random_shadows(
    image: Image.Image,
    np_rng: np.random.Generator,
    py_rng: random.Random,
) -> Image.Image:
    """Apply directional or radial shadows / uneven lighting."""
    w, h = image.size
    nx, ny = _norm_grid(w, h)

    strength = float(np_rng.uniform(0.18, 0.45))
    if py_rng.random() < 0.5:
        angle = float(np_rng.uniform(0.0, 2.0 * np.pi))
        proj = (nx - 0.5) * np.cos(angle) + (ny - 0.5) * np.sin(angle)
        factor_map = 1.0 - strength * (0.5 - 0.5 * np.clip(proj * 1.8, -1.0, 1.0))
    else:
        cx = float(np_rng.uniform(0.2, 0.8))
        cy = float(np_rng.uniform(0.2, 0.8))
        radius = float(np_rng.uniform(0.25, 0.65))
        dist = np.sqrt((nx - cx) ** 2 + (ny - cy) ** 2)
        blob = np.exp(-((dist / radius) ** 2))
        if py_rng.random() < 0.5:
            factor_map = 1.0 - strength * blob
        else:
            factor_map = 1.0 + (strength * 0.7) * blob

    arr = np.asarray(image, dtype=np.float32) * factor_map[..., None]
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), mode="RGB")


def apply_color_jitter(
    image: Image.Image,
    py_rng: random.Random,
) -> Image.Image:
    """Apply brightness, contrast, saturation, and hue shifts."""
    out = image.convert("RGB")
    out = ImageEnhance.Brightness(out).enhance(py_rng.uniform(0.82, 1.18))
    out = ImageEnhance.Contrast(out).enhance(py_rng.uniform(0.82, 1.20))
    out = ImageEnhance.Color(out).enhance(py_rng.uniform(0.78, 1.25))

    hue_shift = int(round(py_rng.uniform(-10.0, 10.0)))
    if hue_shift != 0:
        hsv = np.asarray(out.convert("HSV"), dtype=np.uint8).copy()
        hsv[..., 0] = ((hsv[..., 0].astype(np.int16) + hue_shift) % 256).astype(np.uint8)
        out = Image.fromarray(hsv, mode="HSV").convert("RGB")
    return out


def apply_blur(
    image: Image.Image,
    py_rng: random.Random,
    np_rng: np.random.Generator,
) -> Image.Image:
    """Apply Gaussian blur or fast directional motion blur."""
    if py_rng.random() < 0.65:
        radius = float(np_rng.uniform(0.4, 1.4))
        return image.filter(ImageFilter.GaussianBlur(radius=radius))

    ksize = int(py_rng.choice([5, 7, 9]))
    angle = float(np_rng.uniform(0.0, np.pi))
    center = (ksize - 1) / 2.0
    steps = max(ksize * 2, 10)
    t_vals = np.linspace(-center, center, steps)
    dx_pts = np.rint(center + t_vals * np.cos(angle)).astype(np.int32)
    dy_pts = np.rint(center + t_vals * np.sin(angle)).astype(np.int32)
    valid = (dx_pts >= 0) & (dx_pts < ksize) & (dy_pts >= 0) & (dy_pts < ksize)
    dx_pts = dx_pts[valid]
    dy_pts = dy_pts[valid]

    kernel = np.zeros((ksize, ksize), dtype=np.float32)
    if len(dx_pts) > 0:
        np.add.at(kernel, (dy_pts, dx_pts), 1.0)
        kernel /= kernel.sum()
    else:
        kernel[ksize // 2, ksize // 2] = 1.0

    if ksize == 5:
        pil_kernel = ImageFilter.Kernel(
            size=(5, 5),
            kernel=kernel.ravel().tolist(),
            scale=1.0,
            offset=0,
        )
        return image.filter(pil_kernel)

    arr = np.asarray(image, dtype=np.float32)
    h, w, _ = arr.shape
    pad = ksize // 2
    padded = np.pad(arr, ((pad, pad), (pad, pad), (0, 0)), mode="edge")
    acc = np.zeros_like(arr)
    nz_y, nz_x = np.nonzero(kernel)
    for ky, kx in zip(nz_y.tolist(), nz_x.tolist()):
        acc += float(kernel[ky, kx]) * padded[ky : ky + h, kx : kx + w, :]
    return Image.fromarray(np.clip(acc, 0, 255).astype(np.uint8), mode="RGB")


def _find_perspective_coeffs(
    src_pts: list[tuple[float, float]],
    dst_pts: list[tuple[float, float]],
) -> tuple[float, ...]:
    """Solve perspective transform coefficients mapping src -> dst for PIL."""
    matrix = []
    for (sx, sy), (dx, dy) in zip(src_pts, dst_pts):
        matrix.append([dx, dy, 1.0, 0.0, 0.0, 0.0, -sx * dx, -sx * dy])
        matrix.append([0.0, 0.0, 0.0, dx, dy, 1.0, -sy * dx, -sy * dy])
    a = np.asarray(matrix, dtype=np.float64)
    b = np.asarray([coord for pt in src_pts for coord in pt], dtype=np.float64)
    coeffs = np.linalg.solve(a, b)
    return tuple(float(c) for c in coeffs)


def apply_perspective_or_scale_crop(
    image: Image.Image,
    np_rng: np.random.Generator,
    py_rng: random.Random,
) -> Image.Image:
    """Apply random perspective warp or scale crop back to original dimensions."""
    w, h = image.size
    if py_rng.random() < 0.5:
        scale = float(np_rng.uniform(0.78, 0.98))
        crop_w = max(1, int(round(w * scale)))
        crop_h = max(1, int(round(h * scale)))
        left = int(py_rng.randint(0, max(0, w - crop_w)))
        top = int(py_rng.randint(0, max(0, h - crop_h)))
        cropped = image.crop((left, top, left + crop_w, top + crop_h))
        return cropped.resize((w, h), Image.Resampling.BILINEAR)

    max_shift_x = w * 0.08
    max_shift_y = h * 0.08
    corners = [(0.0, 0.0), (float(w), 0.0), (float(w), float(h)), (0.0, float(h))]
    perturbed = [
        (
            float(np.clip(x + np_rng.uniform(-max_shift_x, max_shift_x), 0.0, float(w))),
            float(np.clip(y + np_rng.uniform(-max_shift_y, max_shift_y), 0.0, float(h))),
        )
        for (x, y) in corners
    ]
    coeffs = _find_perspective_coeffs(corners, perturbed)
    return image.transform(
        (w, h),
        Image.Transform.PERSPECTIVE,
        coeffs,
        resample=Image.Resampling.BILINEAR,
    )


def apply_jpeg_compression(
    image: Image.Image,
    py_rng: random.Random,
) -> Image.Image:
    """Apply mild JPEG compression artifacts."""
    quality = int(py_rng.randint(55, 88))
    buf = io.BytesIO()
    image.save(buf, format="JPEG", quality=quality)
    buf.seek(0)
    with Image.open(buf) as decoded:
        return decoded.convert("RGB")


class FieldAugmentor:
    """Reproducible field-condition augmentation pipeline."""

    def __init__(
        self,
        bg_prob: float = 0.4,
        transform_prob: float = 0.4,
        bg_dir: str | Path | None = DEFAULT_BG_DIR,
        seed: int | None = None,
    ) -> None:
        if not (0.0 <= bg_prob <= 1.0):
            raise ValueError(f"bg_prob must be in [0.0, 1.0], got {bg_prob}")
        if not (0.0 <= transform_prob <= 1.0):
            raise ValueError(f"transform_prob must be in [0.0, 1.0], got {transform_prob}")
        self.bg_prob = float(bg_prob)
        self.transform_prob = float(transform_prob)
        self.bg_dir = Path(bg_dir) if bg_dir is not None else None
        self.bg_paths: list[Path] = discover_background_images(self.bg_dir)
        self.base_seed = seed

    def __call__(
        self,
        image: Image.Image,
        seed: int | None = None,
    ) -> Image.Image:
        effective_seed = seed if seed is not None else self.base_seed
        py_rng, np_rng = make_rng(effective_seed)
        out = image.convert("RGB")

        if py_rng.random() < self.bg_prob:
            out = composite_outdoor_background(out, np_rng, py_rng, bg_paths=self.bg_paths)

        if py_rng.random() < self.transform_prob:
            if py_rng.random() < 0.65:
                out = apply_random_shadows(out, np_rng, py_rng)
            if py_rng.random() < 0.75:
                out = apply_color_jitter(out, py_rng)
            if py_rng.random() < 0.50:
                out = apply_perspective_or_scale_crop(out, np_rng, py_rng)
            if py_rng.random() < 0.45:
                out = apply_blur(out, py_rng, np_rng)
            if py_rng.random() < 0.50:
                out = apply_jpeg_compression(out, py_rng)

        return out


def augment_image_bytes(
    image_bytes: bytes,
    augmentor: FieldAugmentor,
    seed: int | None = None,
) -> Image.Image:
    """Decode raw image bytes into an RGB PIL Image and apply field augmentations."""
    with Image.open(io.BytesIO(image_bytes)) as opened:
        img = ImageOps.exif_transpose(opened).convert("RGB")
    return augmentor(img, seed=seed)
