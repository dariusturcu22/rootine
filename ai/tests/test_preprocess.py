import pytest

from app.inference.preprocess import InvalidImageError, preprocess_image

MEAN = [0.485, 0.456, 0.406]
STD = [0.229, 0.224, 0.225]


def test_preprocess_returns_normalized_nchw_tensor(png_bytes):
    tensor = preprocess_image(png_bytes, 224, MEAN, STD)

    assert tensor.shape == (1, 3, 224, 224)
    assert tensor.dtype.name == "float32"


def test_preprocess_rejects_non_image_bytes():
    with pytest.raises(InvalidImageError):
        preprocess_image(b"nope", 224, MEAN, STD)
