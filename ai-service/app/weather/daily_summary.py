from collections import defaultdict
from dataclasses import dataclass
from datetime import date
from typing import Any


@dataclass(frozen=True)
class DailySummary:
    """Weather of one day, reduced from hourly Open-Meteo data."""

    day: date
    min_temp_c: float
    mean_temp_c: float
    hours_rh_ge_90: int
    total_precip: float = 0.0
    wet_hours_count: int = 0
    mean_temp_during_wet_hours: float | None = None
    longest_continuous_wet_run: int = 0

    @property
    def t_min(self) -> float:
        return self.min_temp_c

    @property
    def t_mean(self) -> float:
        return self.mean_temp_c


@dataclass(frozen=True)
class RuleResult:
    """Output of one weather rule model."""

    level: str
    risk_index: int


def _longest_wet_run(rh_values: list[float], rh_threshold: float) -> int:
    longest = 0
    current = 0
    for rh in rh_values:
        if rh >= rh_threshold:
            current += 1
            if current > longest:
                longest = current
        else:
            current = 0
    return longest


def summarize_days(
    hourly: dict[str, Any],
    rh_threshold: float = 90.0,
) -> list[DailySummary]:
    """Reduce an Open-Meteo hourly response to one summary per local calendar day."""
    data = hourly.get("hourly", hourly)
    times: list[str] = data.get("time", [])
    if not times:
        return []

    temps: list[float] = data["temperature_2m"]
    rhs: list[float] = data["relative_humidity_2m"]
    precips: list[float] = data.get("precipitation", [0.0] * len(times))

    if not (len(times) == len(temps) == len(rhs) == len(precips)):
        raise ValueError("Hourly weather arrays must have the same length")

    grouped: dict[date, list[tuple[float, float, float]]] = defaultdict(list)
    for t_str, temp, rh, precip in zip(times, temps, rhs, precips, strict=True):
        day_key = date.fromisoformat(t_str[:10])
        if temp is not None and rh is not None:
            grouped[day_key].append((float(temp), float(rh), float(precip or 0.0)))

    summaries: list[DailySummary] = []
    for day_key in sorted(grouped):
        records = grouped[day_key]
        if not records:
            continue
        day_temps = [r[0] for r in records]
        day_rhs = [r[1] for r in records]
        day_precips = [r[2] for r in records]

        t_min = min(day_temps)
        t_mean = sum(day_temps) / len(day_temps)
        total_precip = sum(day_precips)

        wet_temps = [t for t, rh in zip(day_temps, day_rhs, strict=True) if rh >= rh_threshold]
        wet_count = len(wet_temps)
        mean_wet_temp = (sum(wet_temps) / wet_count) if wet_count > 0 else None
        longest_run = _longest_wet_run(day_rhs, rh_threshold)
        rh_90_count = sum(1 for rh in day_rhs if rh >= 90.0)

        summaries.append(
            DailySummary(
                day=day_key,
                min_temp_c=t_min,
                mean_temp_c=t_mean,
                hours_rh_ge_90=rh_90_count,
                total_precip=total_precip,
                wet_hours_count=wet_count,
                mean_temp_during_wet_hours=mean_wet_temp,
                longest_continuous_wet_run=longest_run,
            )
        )

    return summaries
