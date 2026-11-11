import io
import struct
import zlib

from PIL import Image

from app.config import Settings
from app.inference.classifier import load_metadata
from app.inference.preprocess import preprocess_image
from tests.conftest import MODEL_DIR

METADATA = load_metadata(MODEL_DIR / "metadata.json")
PNG_IHDR_TYPE_OFFSET = 12
PNG_IHDR_SIZE_OFFSET = 16
PNG_IHDR_END_OFFSET = 29
PNG_SIZE_FIELDS_LENGTH = 8
PNG_CRC_LENGTH = 4
BOMB_WIDTH = 20000
BOMB_HEIGHT = 10000
EXIF_ORIENTATION_TAG = 0x0112
EXIF_ROTATE_CLOCKWISE = 6
RED = (255, 0, 0)
BLUE = (0, 0, 255)
RED_CHANNEL = 0


def post_image(client, content: bytes, crop: str | None = None):
    data = {"crop": crop} if crop is not None else None
    return client.post(
        "/v1/predict", files={"image": ("leaf.png", content, "image/png")}, data=data
    )


def png_with_declared_size(width: int, height: int) -> bytes:
    buffer = io.BytesIO()
    Image.new("RGB", (1, 1)).save(buffer, format="PNG")
    png = bytearray(buffer.getvalue())
    size_fields_end = PNG_IHDR_SIZE_OFFSET + PNG_SIZE_FIELDS_LENGTH
    png[PNG_IHDR_SIZE_OFFSET:size_fields_end] = struct.pack(">II", width, height)
    checksum = zlib.crc32(bytes(png[PNG_IHDR_TYPE_OFFSET:PNG_IHDR_END_OFFSET]))
    png[PNG_IHDR_END_OFFSET : PNG_IHDR_END_OFFSET + PNG_CRC_LENGTH] = struct.pack(">I", checksum)
    return bytes(png)


def jpeg_with_left_red_right_blue_rotated_by_exif() -> bytes:
    image = Image.new("RGB", (64, 32), color=BLUE)
    image.paste(Image.new("RGB", (32, 32), color=RED), (0, 0))
    exif = Image.Exif()
    exif[EXIF_ORIENTATION_TAG] = EXIF_ROTATE_CLOCKWISE
    buffer = io.BytesIO()
    image.save(buffer, format="JPEG", exif=exif)
    return buffer.getvalue()


def test_crop_name_is_trimmed_and_lowercased(client, png_bytes):
    body = post_image(client, png_bytes, "Potato ").json()

    assert body["status"] == "confident"
    assert body["crop"] == "potato"


def test_real_mode_returns_masked_prediction(classifier_client, png_bytes):
    body = post_image(classifier_client, png_bytes, "TOMATO").json()

    assert body["status"] == "confident"
    assert body["crop"] == "tomato"
    assert body["crop_check"] == "ok"
    assert body["top_predictions"][0]["label"] == "tomato_early_blight"


def test_real_mode_refuses_disabled_crop_before_inference(classifier_client, png_bytes):
    body = post_image(classifier_client, png_bytes, "blueberry").json()

    assert body["status"] == "crop_not_supported"
    assert body["top_predictions"] == []


def test_decompression_bomb_is_invalid_image(classifier_client):
    response = post_image(classifier_client, png_with_declared_size(BOMB_WIDTH, BOMB_HEIGHT))

    assert response.json()["status"] == "invalid_image"


def test_file_over_upload_limit_is_invalid_image(classifier_client, png_bytes):
    padding_size = Settings().max_upload_bytes
    response = post_image(classifier_client, png_bytes + bytes(padding_size), "tomato")

    assert response.json()["status"] == "invalid_image"
    assert response.json()["crop_check"] is None


def test_non_image_is_invalid_image(classifier_client):
    body = post_image(classifier_client, b"this is not an image", "tomato").json()

    assert body["status"] == "invalid_image"
    assert body["top_predictions"] == []


def test_exif_orientation_is_applied_before_resizing():
    tensor = preprocess_image(
        jpeg_with_left_red_right_blue_rotated_by_exif(),
        METADATA["input_size"],
        METADATA["mean"],
        METADATA["std"],
    )

    middle_column = METADATA["input_size"] // 2
    top_red = tensor[0, RED_CHANNEL, 0, middle_column]
    bottom_red = tensor[0, RED_CHANNEL, -1, middle_column]
    assert top_red > bottom_red
