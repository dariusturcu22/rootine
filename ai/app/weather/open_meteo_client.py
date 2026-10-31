import httpx


class OpenMeteoClient:
    """Fetches hourly weather from Open-Meteo, with a cache of CACHE_TTL seconds."""

    def __init__(self, base_url: str, cache_ttl: int, http: httpx.AsyncClient | None = None):
        self.base_url = base_url
        self.cache_ttl = cache_ttl
        self._http = http or httpx.AsyncClient(timeout=10)

    async def fetch_hourly(self, latitude: float, longitude: float) -> dict:
        raise NotImplementedError("Open-Meteo client is not implemented yet")
