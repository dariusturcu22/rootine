import time
from datetime import date, timedelta
from typing import Any

import httpx

HOURLY_VARS = "temperature_2m,relative_humidity_2m,dew_point_2m,precipitation"
TIMEZONE = "Europe/Bucharest"
ARCHIVE_URL = "https://archive-api.open-meteo.com/v1/archive"


class OpenMeteoClient:
    """Fetches hourly weather from Open-Meteo, with a cache of CACHE_TTL seconds."""

    def __init__(
        self,
        base_url: str = "https://api.open-meteo.com/v1/forecast",
        cache_ttl: int = 3600,
        http: httpx.AsyncClient | None = None,
        archive_url: str = ARCHIVE_URL,
    ) -> None:
        self.base_url = base_url
        self.archive_url = archive_url
        self.cache_ttl = cache_ttl
        self._http = http or httpx.AsyncClient(timeout=10.0)
        self._cache: dict[tuple[float, float, str], tuple[float, dict[str, Any]]] = {}

    async def fetch_hourly(
        self,
        latitude: float,
        longitude: float,
        replay_date: date | None = None,
    ) -> dict[str, Any]:
        """Fetch 14 days (7 past + 7 forecast/following) of hourly weather in Europe/Bucharest."""
        key = (
            round(latitude, 2),
            round(longitude, 2),
            replay_date.isoformat() if replay_date else "live",
        )
        now = time.monotonic()
        cached = self._cache.get(key)
        if cached is not None and (now - cached[0]) < self.cache_ttl:
            return cached[1]

        if replay_date is None:
            url = self.base_url
            params: dict[str, Any] = {
                "latitude": latitude,
                "longitude": longitude,
                "hourly": HOURLY_VARS,
                "past_days": 7,
                "forecast_days": 7,
                "timezone": TIMEZONE,
            }
        else:
            url = self.archive_url
            start_date = replay_date - timedelta(days=7)
            end_date = replay_date + timedelta(days=6)
            params = {
                "latitude": latitude,
                "longitude": longitude,
                "hourly": HOURLY_VARS,
                "start_date": start_date.isoformat(),
                "end_date": end_date.isoformat(),
                "timezone": TIMEZONE,
            }

        response = await self._http.get(url, params=params)
        response.raise_for_status()
        payload: dict[str, Any] = response.json()
        self._cache[key] = (now, payload)
        return payload
