from datetime import date

import httpx
import pytest
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app
from app.weather.daily_summary import DailySummary, summarize_days
from app.weather.open_meteo_client import OpenMeteoClient
from app.weather.rules import hutton, mills, tomcast
from tests.conftest import MODEL_DIR


def _make_day(
    day_offset: int,
    min_temp_c: float = 12.0,
    hours_rh_ge_90: int = 0,
    wet_hours_count: int = 0,
    mean_wet_temp: float | None = None,
) -> DailySummary:
    return DailySummary(
        day=date(2026, 6, 1 + day_offset),
        min_temp_c=min_temp_c,
        mean_temp_c=min_temp_c + 5.0,
        hours_rh_ge_90=hours_rh_ge_90,
        total_precip=0.0,
        wet_hours_count=wet_hours_count,
        mean_temp_during_wet_hours=mean_wet_temp,
        longest_continuous_wet_run=wet_hours_count,
    )


def test_hutton_high_on_two_consecutive_days():
    days = [
        _make_day(0, min_temp_c=11.0, hours_rh_ge_90=6),
        _make_day(1, min_temp_c=10.5, hours_rh_ge_90=8),
        _make_day(2, min_temp_c=8.0, hours_rh_ge_90=2),
    ]
    res = hutton.assess(days)
    assert res.level == "high"
    assert res.risk_index >= 75


def test_hutton_medium_When_days_are_not_consecutive():
    days = [
        _make_day(0, min_temp_c=12.0, hours_rh_ge_90=7),
        _make_day(1, min_temp_c=7.0, hours_rh_ge_90=2),
        _make_day(2, min_temp_c=13.0, hours_rh_ge_90=6),
    ]
    res = hutton.assess(days)
    assert res.level in {"low", "medium"}
    assert res.risk_index < 75


def test_hutton_low_when_cold_or_dry():
    days = [
        _make_day(0, min_temp_c=8.5, hours_rh_ge_90=10),
        _make_day(1, min_temp_c=9.0, hours_rh_ge_90=12),
    ]
    res = hutton.assess(days)
    assert res.level == "low"
    assert res.risk_index <= 25


def test_tomcast_daily_dsv_brackets():
    assert tomcast.daily_dsv(None, 10) == 0
    assert tomcast.daily_dsv(12.0, 20) == 0
    assert tomcast.daily_dsv(30.0, 20) == 0
    assert tomcast.daily_dsv(15.0, 6) == 0
    assert tomcast.daily_dsv(15.0, 10) == 1
    assert tomcast.daily_dsv(15.0, 18) == 2
    assert tomcast.daily_dsv(15.0, 22) == 3
    assert tomcast.daily_dsv(22.0, 2) == 0
    assert tomcast.daily_dsv(22.0, 4) == 1
    assert tomcast.daily_dsv(22.0, 10) == 2
    assert tomcast.daily_dsv(22.0, 16) == 3
    assert tomcast.daily_dsv(22.0, 22) == 4


def test_tomcast_accumulates_low_medium_high():
    low_days = [_make_day(i, wet_hours_count=2, mean_wet_temp=20.0) for i in range(5)]
    assert tomcast.assess(low_days).level == "low"

    med_days = [_make_day(i, wet_hours_count=10, mean_wet_temp=22.0) for i in range(4)]
    med_res = tomcast.assess(med_days)
    assert med_res.level == "medium"
    assert 35 <= med_res.risk_index < 75

    high_days = [_make_day(i, wet_hours_count=16, mean_wet_temp=22.0) for i in range(6)]
    high_res = tomcast.assess(high_days)
    assert high_res.level == "high"
    assert high_res.risk_index >= 75


def test_summarize_days_carries_wet_period_across_midnight_and_later_spell():
    # Day 1 has 3 wet hours at 21:00..23:00.
    # Day 2 has 2 leading wet hours at 00:00..01:00 AND 4 later wet hours at 10:00..13:00.
    # Carried overnight hours (3) + in-day RH>=90 hours (2 + 4 = 6) = 9 total hours_rh_ge_90.
    times = [f"2026-06-10T{h:02d}:00" for h in range(24)] + [
        f"2026-06-11T{h:02d}:00" for h in range(24)
    ]
    temps = [15.0] * 48
    rhs_day1 = [60.0] * 21 + [92.0, 93.0, 91.0]
    rhs_day2 = [94.0, 92.0] + [55.0] * 8 + [91.0, 93.0, 95.0, 90.0] + [55.0] * 10
    hourly = {
        "hourly": {
            "time": times,
            "temperature_2m": temps,
            "relative_humidity_2m": rhs_day1 + rhs_day2,
            "precipitation": [0.0] * 48,
        }
    }

    summaries = summarize_days(hourly)
    assert len(summaries) == 2
    assert summaries[0].hours_rh_ge_90 == 3
    assert summaries[1].longest_continuous_wet_run == 5
    assert summaries[1].hours_rh_ge_90 == 9
    assert summaries[1].wet_hours_count == 9


def test_summarize_days_handles_missing_temp_and_rh_breaking_wet_run():
    # Hour 2 has temp=None but rh=95.0 -> must still break the wet run!
    times = [f"2026-06-10T{h:02d}:00" for h in range(6)]
    temps = [14.0, 15.0, None, 16.0, 17.0, 18.0]
    rhs = [95.0, 95.0, 95.0, 95.0, 95.0, 95.0]
    hourly = {
        "hourly": {
            "time": times,
            "temperature_2m": temps,
            "relative_humidity_2m": rhs,
            "precipitation": [0.0, None, 1.0, 0.0, 0.0, 0.0],
        }
    }
    summaries = summarize_days(hourly)
    assert len(summaries) == 1
    assert summaries[0].longest_continuous_wet_run == 3
    assert summaries[0].wet_hours_count == 5


@pytest.mark.anyio
async def test_open_meteo_client_retries_on_503():
    attempts = 0

    def handler(request: httpx.Request) -> httpx.Response:
        nonlocal attempts
        attempts += 1
        if attempts < 3:
            return httpx.Response(503, text="Service Unavailable")
        return httpx.Response(
            200,
            json={
                "hourly": {
                    "time": ["2026-06-10T00:00"],
                    "temperature_2m": [16.0],
                    "relative_humidity_2m": [92.0],
                    "precipitation": [0.0],
                }
            },
        )

    transport = httpx.MockTransport(handler)
    async with httpx.AsyncClient(transport=transport) as http:
        client = OpenMeteoClient(http=http, max_retries=3, retry_delay=0.0)
        payload = await client.fetch_hourly(46.77, 23.59)

    assert attempts == 3
    assert payload["hourly"]["temperature_2m"] == [16.0]


def test_live_risk_endpoint_computes_real_hutton_and_tomcast():
    times = [f"2026-06-{10 + d:02d}T{h:02d}:00" for d in range(3) for h in range(24)]
    hourly_payload = {
        "hourly": {
            "time": times,
            "temperature_2m": [21.0] * len(times),
            "relative_humidity_2m": [94.0] * len(times),
            "precipitation": [0.2] * len(times),
        }
    }

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json=hourly_payload)

    settings = Settings(ai_mock=True, model_dir=MODEL_DIR)
    app = create_app(settings)
    with TestClient(app) as client:
        app.state.mock = False
        app.state.weather_client = OpenMeteoClient(
            http=httpx.AsyncClient(transport=httpx.MockTransport(handler)),
            retry_delay=0.0,
        )
        res = client.post(
            "/v1/risk",
            json={"latitude": 46.77, "longitude": 23.59, "crop": "tomato"},
        )

    assert res.status_code == 200
    body = res.json()
    assert body["crop"] == "tomato"
    assert body["level"] == "high"
    assert body["risk_index"] >= 75
    models_used = {d["model"] for d in body["diseases"]}
    assert models_used == {"hutton", "tomcast"}
    assert "Mock response" not in body["reason"]


def test_mills_daily_severity_brackets():
    assert mills.daily_mills_severity(None, 20) == 0
    assert mills.daily_mills_severity(1.5, 24) == 0
    assert mills.daily_mills_severity(27.0, 24) == 0
    # 6-8 C requires >= 15h light, >= 20h moderate, >= 30h heavy
    assert mills.daily_mills_severity(7.0, 14) == 0
    assert mills.daily_mills_severity(7.0, 16) == 1
    assert mills.daily_mills_severity(7.0, 20) == 2
    assert mills.daily_mills_severity(7.0, 30) == 3
    # Optimal 16-24 C requires >= 9h light, >= 12h moderate, >= 18h heavy
    assert mills.daily_mills_severity(18.0, 8) == 0
    assert mills.daily_mills_severity(18.0, 9) == 1
    assert mills.daily_mills_severity(18.0, 12) == 2
    assert mills.daily_mills_severity(18.0, 18) == 3


def test_mills_assess_low_medium_high():
    # Cold / short wet spells -> low
    low_days = [
        _make_day(0, min_temp_c=4.0, wet_hours_count=5, mean_wet_temp=7.0),
        _make_day(1, min_temp_c=10.0, wet_hours_count=6, mean_wet_temp=18.0),
    ]
    low_res = mills.assess(low_days)
    assert low_res.level == "low"
    assert low_res.risk_index < 40

    # Borderline single light/moderate infection spell (10h at 18 C) -> medium
    med_days = [
        _make_day(0, min_temp_c=14.0, wet_hours_count=10, mean_wet_temp=18.0),
        _make_day(1, min_temp_c=12.0, wet_hours_count=2, mean_wet_temp=15.0),
    ]
    med_res = mills.assess(med_days)
    assert med_res.level == "medium"
    assert 40 <= med_res.risk_index < 75

    # Prolonged warm wet spell (19h continuous at 19 C) -> high
    high_days = [
        _make_day(0, min_temp_c=15.0, wet_hours_count=19, mean_wet_temp=19.0),
        _make_day(1, min_temp_c=14.0, wet_hours_count=12, mean_wet_temp=18.0),
    ]
    high_res = mills.assess(high_days)
    assert high_res.level == "high"
    assert high_res.risk_index >= 75


def test_live_risk_endpoint_computes_real_mills_for_apple():
    times = [f"2026-06-{10 + d:02d}T{h:02d}:00" for d in range(2) for h in range(24)]
    hourly_payload = {
        "hourly": {
            "time": times,
            "temperature_2m": [19.0] * len(times),
            "relative_humidity_2m": [94.0] * len(times),
            "precipitation": [0.4] * len(times),
        }
    }

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json=hourly_payload)

    settings = Settings(ai_mock=True, model_dir=MODEL_DIR)
    app = create_app(settings)
    with TestClient(app) as client:
        app.state.mock = False
        app.state.weather_client = OpenMeteoClient(
            http=httpx.AsyncClient(transport=httpx.MockTransport(handler)),
            retry_delay=0.0,
        )
        res = client.post(
            "/v1/risk",
            json={"latitude": 46.77, "longitude": 23.59, "crop": "apple"},
        )

    assert res.status_code == 200
    body = res.json()
    assert body["crop"] == "apple"
    assert body["level"] == "high"
    assert body["risk_index"] >= 75
    assert [d["model"] for d in body["diseases"]] == ["mills"]
    assert [d["disease"] for d in body["diseases"]] == ["apple_scab"]
