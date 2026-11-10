from datetime import UTC, datetime
from typing import cast

import httpx
from fastapi import APIRouter, HTTPException, Request

from app.schemas import DiseaseRisk, RiskLevel, RiskRequest, RiskResponse
from app.weather.daily_summary import DailySummary, RuleResult, summarize_days
from app.weather.registry import WEATHER_RULES

router = APIRouter()

LEVEL_PRIORITY: dict[str, int] = {
    "not_modelled": 0,
    "low": 1,
    "medium": 2,
    "high": 3,
}


def _format_reason(
    crop: str, level: RiskLevel, diseases: list[DiseaseRisk], days_count: int
) -> str:
    """Build a human-readable explanation of the computed weather risk."""
    if not diseases or days_count == 0:
        return f"Insufficient hourly weather data to evaluate disease risk for {crop}."

    parts = [
        f"{d.disease.replace('_', ' ')} ({d.model.upper()}): {d.level} ({d.risk_index}/100)"
        for d in diseases
    ]
    summary_str = ", ".join(parts)
    return (
        f"Overall {crop} weather risk is {level} over the {days_count}-day window [{summary_str}]."
    )


def _evaluate_crop_diseases(
    crop: str,
    labels: list,
    allowed_rules: tuple[str, ...],
    days: list[DailySummary],
) -> list[DiseaseRisk]:
    """Evaluate all diseases of a crop that have an active weather rule."""
    rule_cache: dict[str, RuleResult] = {}
    results: list[DiseaseRisk] = []
    seen: set[tuple[str, str]] = set()

    for label in labels:
        if label.crop != crop or not label.weather_rule or label.weather_rule == "none":
            continue
        rule_name = label.weather_rule
        if rule_name not in allowed_rules or rule_name not in WEATHER_RULES:
            continue
        if (label.disease, rule_name) in seen:
            continue
        seen.add((label.disease, rule_name))

        if rule_name not in rule_cache:
            rule_cache[rule_name] = WEATHER_RULES[rule_name](days)
        res = rule_cache[rule_name]
        results.append(
            DiseaseRisk(
                disease=label.disease,
                model=rule_name,
                level=cast(RiskLevel, res.level),
                risk_index=res.risk_index,
            )
        )
    return results


@router.post("/v1/risk", response_model=RiskResponse)
async def risk(body: RiskRequest, request: Request) -> RiskResponse:
    state = request.app.state
    config = state.crops.get(body.crop)

    if not (config and config.enabled and config.weather_rules):
        reason = (
            "Mock response: no weather model exists for this crop."
            if state.mock
            else f"No weather risk model is configured for crop '{body.crop}'."
        )
        return RiskResponse(
            crop=body.crop,
            level="not_modelled",
            risk_index=0,
            reason=reason,
            diseases=[],
            generated_at=datetime.now(UTC),
        )

    if state.mock:
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

    try:
        hourly = await state.weather_client.fetch_hourly(
            latitude=body.latitude,
            longitude=body.longitude,
            replay_date=body.replay_date,
        )
    except httpx.HTTPError as exc:
        raise HTTPException(
            status_code=502,
            detail=f"Failed to fetch weather data from Open-Meteo: {exc}",
        ) from exc

    days = summarize_days(hourly)
    diseases = _evaluate_crop_diseases(body.crop, state.labels, config.weather_rules, days)

    if not diseases:
        return RiskResponse(
            crop=body.crop,
            level="not_modelled",
            risk_index=0,
            reason=f"No active weather rules matched for crop '{body.crop}'.",
            diseases=[],
            generated_at=datetime.now(UTC),
        )

    overall_level = max(diseases, key=lambda d: LEVEL_PRIORITY.get(d.level, 0)).level
    overall_index = max(d.risk_index for d in diseases)

    return RiskResponse(
        crop=body.crop,
        level=overall_level,
        risk_index=overall_index,
        reason=_format_reason(body.crop, overall_level, diseases, len(days)),
        diseases=diseases,
        generated_at=datetime.now(UTC),
    )
