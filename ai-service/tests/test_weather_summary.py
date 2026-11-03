import asyncio
from datetime import date

import httpx
import pytest

from app.weather.daily_summary import summarize_days
from app.weather.open_meteo_client import OpenMeteoClient


def _make_hourly_payload() -> dict:
    day1_times = [f"2026-06-01T{h:02d}:00" for h in range(24)]
    day2_times = [f"2026-06-02T{h:02d}:00" for h in range(24)]

    # Day 1: 6 consecutive wet hours (00:00..05:00 at RH=92%, temp=15C), rest RH=60%, temp=20C
    day1_temps = [15.0] * 6 + [20.0] * 18
    day1_rhs = [92.0] * 6 + [60.0] * 18
    day1_precips = [0.5] * 6 + [0.0] * 18

    # Day 2: two wet runs (2 hours at RH=95%, then 4 hours at RH=88%)
    day2_temps = [12.0] * 2 + [18.0] * 4 + [16.0] * 4 + [22.0] * 14
    day2_rhs = [95.0] * 2 + [50.0] * 4 + [88.0] * 4 + [55.0] * 14
    day2_precips = [0.0] * 24

    return {
        "timezone": "Europe/Bucharest",
        "hourly": {
            "time": day1_times + day2_times,
            "temperature_2m": day1_temps + day2_temps,
            "relative_humidity_2m": day1_rhs + day2_rhs,
            "dew_point_2m": [10.0] * 48,
            "precipitation": day1_precips + day2_precips,
        },
    }


def test_summarize_days_default_90_threshold():
    payload = _make_hourly_payload()
    days = summarize_days(payload)

    assert len(days) == 2

    d1 = days[0]
    assert d1.day == date(2026, 6, 1)
    assert d1.t_min == pytest.approx(15.0)
    assert d1.t_mean == pytest.approx((15.0 * 6 + 20.0 * 18) / 24)
    assert d1.total_precip == pytest.approx(3.0)
    assert d1.wet_hours_count == 6
    assert d1.hours_rh_ge_90 == 6
    assert d1.mean_temp_during_wet_hours == pytest.approx(15.0)
    assert d1.longest_continuous_wet_run == 6

    d2 = days[1]
    assert d2.day == date(2026, 6, 2)
    assert d2.wet_hours_count == 2
    assert d2.longest_continuous_wet_run == 2


def test_summarize_days_custom_85_threshold_for_spotts():
    payload = _make_hourly_payload()
    days = summarize_days(payload, rh_threshold=85.0)

    d2 = days[1]
    # At 85% threshold, both the 2h (95%) and 4h (88%) runs count as wet
    assert d2.wet_hours_count == 6
    assert d2.longest_continuous_wet_run == 4
    assert d2.hours_rh_ge_90 == 2
    assert d2.mean_temp_during_wet_hours == pytest.approx((12.0 * 2 + 16.0 * 4) / 6)


def test_summarize_days_dry_day_has_none_wet_mean_temp():
    payload = {
        "hourly": {
            "time": ["2026-06-03T00:00", "2026-06-03T01:00"],
            "temperature_2m": [20.0, 22.0],
            "relative_humidity_2m": [40.0, 50.0],
            "precipitation": [0.0, 0.0],
        }
    }
    days = summarize_days(payload)
    assert len(days) == 1
    assert days[0].wet_hours_count == 0
    assert days[0].mean_temp_during_wet_hours is None
    assert days[0].longest_continuous_wet_run == 0


def test_open_meteo_client_forecast_and_cache():
    calls: list[httpx.Request] = []

    def handler(request: httpx.Request) -> httpx.Response:
        calls.append(request)
        return httpx.Response(200, json=_make_hourly_payload())

    async def run_check() -> tuple[dict, dict]:
        transport = httpx.MockTransport(handler)
        async with httpx.AsyncClient(transport=transport) as http:
            client = OpenMeteoClient(
                base_url="https://api.open-meteo.com/v1/forecast",
                cache_ttl=3600,
                http=http,
            )
            # First call hits network; second call within same 0.01 deg grid hits cache
            r1 = await client.fetch_hourly(46.771, 23.591)
            r2 = await client.fetch_hourly(46.774, 23.594)
            return r1, r2

    res1, res2 = asyncio.run(run_check())

    assert len(calls) == 1
    assert calls[0].url.params["timezone"] == "Europe/Bucharest"
    assert calls[0].url.params["past_days"] == "7"
    assert calls[0].url.params["forecast_days"] == "7"
    assert "dew_point_2m" in calls[0].url.params["hourly"]
    assert res1 == res2


def test_open_meteo_client_replay_date_uses_archive_api():
    calls: list[httpx.Request] = []

    def handler(request: httpx.Request) -> httpx.Response:
        calls.append(request)
        return httpx.Response(200, json=_make_hourly_payload())

    async def run_check() -> None:
        transport = httpx.MockTransport(handler)
        async with httpx.AsyncClient(transport=transport) as http:
            client = OpenMeteoClient(http=http)
            await client.fetch_hourly(46.77, 23.59, replay_date=date(2026, 6, 15))

    asyncio.run(run_check())

    assert len(calls) == 1
    assert str(calls[0].url).startswith("https://archive-api.open-meteo.com/v1/archive")
    assert calls[0].url.params["start_date"] == "2026-06-08"
    assert calls[0].url.params["end_date"] == "2026-06-21"
    assert calls[0].url.params["timezone"] == "Europe/Bucharest"
