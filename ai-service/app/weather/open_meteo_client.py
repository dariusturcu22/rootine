import asyncio
import time
from datetime import date, timedelta
from typing import Any

import httpx

HOURLY_VARS = "temperature_2m,relative_humidity_2m,dew_point_2m,precipitation"
TIMEZONE = "Europe/Bucharest"
ARCHIVE_URL = "https://archive-api.open-meteo.com/v1/archive"


class OpenMeteoClient:
    """Fetches hourly weather from Open-Meteo, with caching and retry logic."""

    def __init__(
        self,
        base_url: str = "https://api.open-meteo.com/v1/forecast",
        cache_ttl: int = 3600,
        http: httpx.AsyncClient | None = None,
        archive_url: str = ARCHIVE_URL,
        max_retries: int = 3,
        retry_delay: float = 0.5,
    ) -> None:
        self.base_url = base_url
        self.archive_url = archive_url
        self.cache_ttl = cache_ttl
        self.max_retries = max(1, max_retries)
        self.retry_delay = retry_delay
        self._owns_http = http is None
        self._http = http or httpx.AsyncClient(timeout=10.0)
        self._cache: dict[tuple[float, float, str], tuple[float, dict[str, Any]]] = {}

    async def aclose(self) -> None:
        """Close the underlying HTTP client if owned by this instance."""
        if self._owns_http:
            await self._http.aclose()

    async def _get_with_retry(self, url: str, params: dict[str, Any]) -> dict[str, Any]:
        """Perform GET request with retries on transient network or 5xx errors."""
        last_error: Exception | None = None
        for attempt in range(self.max_retries):
            try:
                response = await self._http.get(url, params=params)
                if response.status_code >= 500 and attempt + 1 < self.max_retries:
                    await asyncio.sleep(self.retry_delay * (2**attempt))
                    continue
                response.raise_for_status()
                payload: dict[str, Any] = response.json()
                return payload
            except (httpx.TransportError, httpx.HTTPStatusError) as exc:
                last_error = exc
                if isinstance(exc, httpx.HTTPStatusError) and exc.response.status_code < 500:
                    raise
                if attempt + 1 < self.max_retries:
                    await asyncio.sleep(self.retry_delay * (2**attempt))
        assert last_error is not None
        raise last_error

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

        payload = await self._get_with_retry(url, params)
        self._cache[key] = (now, payload)
        return payload
