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


def _longest_wet_run(
    rh_values: list[float | None],
    rh_threshold: float,
    carry_in_run: int = 0,
) -> tuple[int, int]:
    """Return (longest_continuous_wet_run, trailing_wet_run_at_end_of_day)."""
    longest = carry_in_run
    current = carry_in_run
    for rh in rh_values:
        if rh is not None and rh >= rh_threshold:
            current += 1
            if current > longest:
                longest = current
        else:
            current = 0
    return longest, current


def _leading_wet_hours(
    temps: list[float | None],
    rhs: list[float | None],
    rh_threshold: float,
) -> list[float]:
    """Return valid temperatures from the continuous wet streak starting at 00:00."""
    leading: list[float] = []
    for temp, rh in zip(temps, rhs, strict=True):
        if temp is not None and rh is not None and rh >= rh_threshold:
            leading.append(temp)
        else:
            break
    return leading


def _trailing_wet_hours(
    temps: list[float | None],
    rhs: list[float | None],
    rh_threshold: float,
) -> list[float]:
    """Return valid temperatures from the continuous wet streak ending at 23:00."""
    trailing: list[float] = []
    for temp, rh in zip(reversed(temps), reversed(rhs), strict=True):
        if temp is not None and rh is not None and rh >= rh_threshold:
            trailing.append(temp)
        else:
            break
    trailing.reverse()
    return trailing


def summarize_days(
    hourly: dict[str, Any],
    rh_threshold: float = 90.0,
    min_valid_hours: int = 1,
) -> list[DailySummary]:
    """Reduce an Open-Meteo hourly response to one summary per local calendar day.

    Handles missing (None) hourly values and carries continuous wet periods that
    cross midnight (23:00 -> 00:00) into the following day's continuous wet run
    and overnight wetness metrics.
    """
    data = hourly.get("hourly", hourly)
    times: list[str] = data.get("time", [])
    if not times:
        return []

    temps: list[float | None] = data["temperature_2m"]
    rhs: list[float | None] = data["relative_humidity_2m"]
    precips: list[float | None] = data.get("precipitation", [0.0] * len(times))

    if not (len(times) == len(temps) == len(rhs) == len(precips)):
        raise ValueError("Hourly weather arrays must have the same length")

    grouped: dict[date, list[tuple[float | None, float | None, float]]] = defaultdict(list)
    for t_str, temp, rh, precip in zip(times, temps, rhs, precips, strict=True):
        day_key = date.fromisoformat(t_str[:10])
        temp_val = float(temp) if temp is not None else None
        rh_val = float(rh) if rh is not None else None
        precip_val = float(precip) if precip is not None else 0.0
        grouped[day_key].append((temp_val, rh_val, precip_val))

    summaries: list[DailySummary] = []
    prev_day: date | None = None
    prev_trailing_wet_temps: list[float] = []

    for day_key in sorted(grouped):
        raw_records = grouped[day_key]
        raw_temps = [r[0] for r in raw_records]
        raw_rhs = [r[1] for r in raw_records]
        valid_pairs = [(t, rh, p) for t, rh, p in raw_records if t is not None and rh is not None]
        if len(valid_pairs) < min_valid_hours:
            prev_day = day_key
            prev_trailing_wet_temps = []
            continue

        is_consecutive = prev_day is not None and (day_key - prev_day).days == 1
        carry_temps = prev_trailing_wet_temps if is_consecutive else []
        leading_wet = _leading_wet_hours(raw_temps, raw_rhs, rh_threshold)
        overnight_temps = carry_temps if leading_wet else []

        day_temps = [r[0] for r in valid_pairs]
        day_rhs = [r[1] for r in valid_pairs]
        day_precips = [r[2] for r in valid_pairs]

        t_min = min(day_temps)
        t_mean = sum(day_temps) / len(day_temps)
        total_precip = sum(day_precips)

        in_day_wet_temps = [
            t for t, rh in zip(day_temps, day_rhs, strict=True) if rh >= rh_threshold
        ]
        effective_wet_temps = overnight_temps + in_day_wet_temps
        wet_count = min(24, len(effective_wet_temps))
        mean_wet_temp = (
            sum(effective_wet_temps) / len(effective_wet_temps) if effective_wet_temps else None
        )

        longest_run, _ = _longest_wet_run(raw_rhs, rh_threshold, carry_in_run=len(overnight_temps))
        in_day_rh90 = sum(1 for rh in day_rhs if rh >= 90.0)
        overnight_rh90 = len(overnight_temps) if rh_threshold >= 90.0 else 0
        rh_90_count = min(24, max(in_day_rh90, len(leading_wet) + overnight_rh90))

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

        prev_day = day_key
        prev_trailing_wet_temps = _trailing_wet_hours(raw_temps, raw_rhs, rh_threshold)

    return summaries
