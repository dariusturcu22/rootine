from typing import Annotated

from fastapi import APIRouter, File, Form, HTTPException, Request, UploadFile

from app.inference.preprocess import InvalidImageError, preprocess_image
from app.schemas import Prediction, PredictResponse

router = APIRouter()

# Fixed mock answer: the same three diseases exist for every enabled crop.
MOCK_PREDICTIONS = [("early_blight", 0.92), ("late_blight", 0.05), ("healthy", 0.03)]
MOCK_DEFAULT_CROP = "tomato"


@router.post("/v1/predict", response_model=PredictResponse)
async def predict(
    request: Request,
    image: Annotated[UploadFile, File()],
    crop: Annotated[str | None, Form()] = None,
) -> PredictResponse:
    state = request.app.state
    if not state.mock:
        raise HTTPException(status_code=501, detail="Real inference is not implemented yet")

    version = state.metadata["model_version"]
    try:
        preprocess_image(
            await image.read(),
            state.metadata["input_size"],
            state.metadata["mean"],
            state.metadata["std"],
        )
    except InvalidImageError:
        return PredictResponse(
            status="invalid_image",
            crop=crop,
            crop_check=None,
            top_predictions=[],
            support_level="not_supported",
            model_version=version,
        )

    config = state.crops.get(crop) if crop else None
    if crop and not (config and config.enabled and config.diagnosis_supported):
        return PredictResponse(
            status="crop_not_supported",
            crop=crop,
            crop_check=None,
            top_predictions=[],
            support_level="not_supported",
            model_version=version,
        )

    chosen = crop or MOCK_DEFAULT_CROP
    return PredictResponse(
        status="confident",
        crop=chosen,
        crop_check="ok",
        top_predictions=[
            Prediction(label=f"{chosen}_{disease}", disease=disease, probability=probability)
            for disease, probability in MOCK_PREDICTIONS
        ],
        support_level=state.crops[chosen].support_level,
        model_version=version,
    )
