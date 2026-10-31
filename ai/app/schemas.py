from datetime import date, datetime
from typing import Literal

from pydantic import BaseModel, Field

SupportLevel = Literal["lab_and_field_tested", "lab_only", "not_supported"]
RiskLevel = Literal["low", "medium", "high", "not_modelled"]


class HealthResponse(BaseModel):
    status: Literal["ok"]
    model_version: str


class CropInfo(BaseModel):
    crop: str
    diagnosis_supported: bool
    weather_supported: bool
    support_level: SupportLevel
    weather_rules: list[str]
    classes: list[str]


class Prediction(BaseModel):
    label: str
    disease: str
    probability: float


class PredictResponse(BaseModel):
    status: Literal["confident", "unsure", "invalid_image", "crop_not_supported"]
    crop: str | None
    crop_check: Literal["ok", "mismatch"] | None
    top_predictions: list[Prediction]
    support_level: SupportLevel
    model_version: str


class RiskRequest(BaseModel):
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    crop: str
    replay_date: date | None = None


class DiseaseRisk(BaseModel):
    disease: str
    model: str
    level: RiskLevel
    risk_index: int = Field(ge=0, le=100)


class RiskResponse(BaseModel):
    crop: str
    level: RiskLevel
    risk_index: int = Field(ge=0, le=100)
    reason: str
    diseases: list[DiseaseRisk]
    generated_at: datetime


class ThreatResponse(BaseModel):
    level: RiskLevel
    reason: str
