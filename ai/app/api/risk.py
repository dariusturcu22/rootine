from datetime import UTC, datetime

from fastapi import APIRouter, HTTPException, Request

from app.schemas import DiseaseRisk, RiskRequest, RiskResponse

router = APIRouter()


@router.post("/v1/risk", response_model=RiskResponse)
def risk(body: RiskRequest, request: Request) -> RiskResponse:
    state = request.app.state
    if not state.mock:
        raise HTTPException(status_code=501, detail="Weather risk is not implemented yet")

    config = state.crops.get(body.crop)
    if not (config and config.enabled and config.weather_rules):
        return RiskResponse(
            crop=body.crop,
            level="not_modelled",
            risk_index=0,
            reason="Mock response: no weather model exists for this crop.",
            diseases=[],
            generated_at=datetime.now(UTC),
        )

    return RiskResponse(
        crop=body.crop,
        level="medium",
        risk_index=42,
        reason="Mock response: fixed medium risk, real weather rules are not implemented yet.",
        diseases=[
            DiseaseRisk(disease="late_blight", model="hutton", level="medium", risk_index=42)
        ],
        generated_at=datetime.now(UTC),
    )
