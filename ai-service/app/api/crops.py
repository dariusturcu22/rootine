from fastapi import APIRouter, Request

from app.schemas import CropInfo

router = APIRouter()


@router.get("/v1/crops", response_model=list[CropInfo])
def list_crops(request: Request) -> list[CropInfo]:
    labels = request.app.state.labels
    return [
        CropInfo(
            crop=name,
            diagnosis_supported=cfg.enabled and cfg.diagnosis_supported,
            weather_supported=cfg.enabled and bool(cfg.weather_rules),
            support_level=cfg.support_level,
            weather_rules=list(cfg.weather_rules),
            classes=[label.disease for label in labels if label.crop == name],
        )
        for name, cfg in request.app.state.crops.items()
        if cfg.enabled
    ]
