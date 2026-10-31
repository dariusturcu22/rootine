from dataclasses import dataclass
from datetime import date


@dataclass(frozen=True)
class DailySummary:
    """Weather of one day, reduced from hourly Open-Meteo data."""

    day: date
    min_temp_c: float
    mean_temp_c: float
    hours_rh_ge_90: int


@dataclass(frozen=True)
class RuleResult:
    """Output of one weather rule model."""

    level: str
    risk_index: int


def summarize_days(hourly: dict) -> list[DailySummary]:
    """Reduce an Open-Meteo hourly response to one summary per day."""
    raise NotImplementedError("Daily summary is not implemented yet")
