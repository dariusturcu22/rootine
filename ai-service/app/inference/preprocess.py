import io

import numpy as np
from PIL import Image, ImageOps, UnidentifiedImageError


class InvalidImageError(ValueError):
    """Raised when the uploaded bytes are not a readable image."""


def preprocess_image(data: bytes, size: int, mean: list[float], std: list[float]) -> np.ndarray:
    """Decode an image and return a float32 tensor of shape (1, 3, size, size)."""
    try:
        with Image.open(io.BytesIO(data)) as opened_image:
            image = ImageOps.exif_transpose(opened_image).convert("RGB")
    except (UnidentifiedImageError, OSError, Image.DecompressionBombError) as error:
        raise InvalidImageError("Cannot read image") from error
    image = image.resize((size, size))
    array = np.asarray(image, dtype=np.float32) / 255.0
    array = (array - np.asarray(mean, dtype=np.float32)) / np.asarray(std, dtype=np.float32)
    return array.transpose(2, 0, 1)[np.newaxis, ...].astype(np.float32)
