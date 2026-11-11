from typing import Annotated, Any, Literal

import numpy as np
from fastapi import APIRouter, File, Form, Request, UploadFile
from fastapi.concurrency import run_in_threadpool
from starlette.datastructures import State

from app.inference.preprocess import InvalidImageError, preprocess_image
from app.schemas import Prediction, PredictResponse

router = APIRouter()

MOCK_PREDICTIONS = [("early_blight", 0.92), ("late_blight", 0.05), ("healthy", 0.03)]
MOCK_DEFAULT_CROP = "tomato"


def normalize_crop(crop: str | None) -> str | None:
    normalized_crop = (crop or "").strip().lower()
    return normalized_crop or None


def is_crop_supported(state: State, crop: str) -> bool:
    config = state.crops.get(crop)
    if not (config and config.enabled and config.diagnosis_supported):
        return False
    return state.classifier is None or state.classifier.has_classes_for(crop)


async def read_upload(image: UploadFile, max_upload_bytes: int) -> bytes:
    image_bytes = await image.read(max_upload_bytes + 1)
    if len(image_bytes) > max_upload_bytes:
        raise InvalidImageError("Image is larger than the upload limit")
    return image_bytes


def build_tensor(image_bytes: bytes, metadata: dict[str, Any]) -> np.ndarray:
    return preprocess_image(image_bytes, metadata["input_size"], metadata["mean"], metadata["std"])


def empty_response(
    status: Literal["invalid_image", "crop_not_supported"], crop: str | None, version: str
) -> PredictResponse:
    return PredictResponse(
        status=status,
        crop=crop,
        crop_check=None,
        top_predictions=[],
        support_level="not_supported",
        model_version=version,
    )


def mock_response(state: State, crop: str | None, version: str) -> PredictResponse:
    chosen_crop = crop or MOCK_DEFAULT_CROP
    return PredictResponse(
        status="confident",
        crop=chosen_crop,
        crop_check="ok",
        top_predictions=[
            Prediction(label=f"{chosen_crop}_{disease}", disease=disease, probability=probability)
            for disease, probability in MOCK_PREDICTIONS
        ],
        support_level=state.crops[chosen_crop].support_level,
        model_version=version,
    )


@router.post("/v1/predict", response_model=PredictResponse)
async def predict(
    request: Request,
    image: Annotated[UploadFile, File()],
    crop: Annotated[str | None, Form()] = None,
) -> PredictResponse:
    state = request.app.state
    requested_crop = normalize_crop(crop)
    version = state.metadata["model_version"]
    try:
        image_bytes = await read_upload(image, state.settings.max_upload_bytes)
        tensor = await run_in_threadpool(build_tensor, image_bytes, state.metadata)
    except InvalidImageError:
        return empty_response("invalid_image", requested_crop, version)
    if requested_crop and not is_crop_supported(state, requested_crop):
        return empty_response("crop_not_supported", requested_crop, version)
    if state.classifier is None:
        return mock_response(state, requested_crop, version)
    result = await run_in_threadpool(state.classifier.predict, tensor, requested_crop)
    return PredictResponse(
        status=result.status,
        crop=result.crop,
        crop_check=result.crop_check,
        top_predictions=result.top_predictions,
        support_level=state.crops[result.crop].support_level,
        model_version=version,
    )
