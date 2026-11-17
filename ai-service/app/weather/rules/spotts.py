from app.weather.daily_summary import DailySummary, RuleResult

MIN_INFECT_TEMP_C = 10.0
MAX_INFECT_TEMP_C = 32.0

TEMP_13 = 13.0
TEMP_15_5 = 15.5
TEMP_18_5 = 18.5
TEMP_21 = 21.0
TEMP_26_5 = 26.5
TEMP_29 = 29.0

HOURS_24 = 24
HOURS_12 = 12
HOURS_9 = 9
HOURS_8 = 8
HOURS_6 = 6

NEAR_MISS_MARGIN = 3
MIN_EVENTS_HIGH = 2
EXCESS_HOURS_HIGH = 6


def required_wet_hours(temp_c: float) -> int:
    """Return minimum continuous wet hours (RH >= 85%) required for grape black rot infection.

    Based on the Spotts (1977) infection curve for Guignardia bidwellii:
    - 10.0 C -> 24 h
    - 13.0 C -> 12 h
    - 15.5 C -> 9 h
    - 18.5 C -> 8 h
    - 21.0 - 26.5 C -> 6 h (optimal)
    - 29.0 C -> 9 h
    - 32.0 C -> 12 h
    """
    if temp_c < MIN_INFECT_TEMP_C or temp_c > MAX_INFECT_TEMP_C:
        return 999
    if temp_c < TEMP_13:
        return HOURS_24
    if temp_c < TEMP_15_5:
        return HOURS_12
    if temp_c < TEMP_18_5:
        return HOURS_9
    if temp_c < TEMP_21:
        return HOURS_8
    if temp_c <= TEMP_26_5:
        return HOURS_6
    if temp_c <= TEMP_29:
        return HOURS_9
    return HOURS_12


def assess(days: list[DailySummary]) -> RuleResult:
    """Grape black rot (Guignardia bidwellii) risk using the Spotts model.

    Evaluates mean temperature during wet hours against minimum continuous
    leaf wetness hours (RH >= 85%) required for infection.

    Mapping to risk level:
    - High (75..100): 2 or more infection days, OR one infection day exceeding
      the minimum required wet hours by >= 6 hours.
    - Medium (40..74): Exactly 1 marginal/standard infection day.
    - Low (0..39): No infection days met.

    Sources:
    - Spotts, R. A. (1977), "Effect of leaf wetness duration and temperature
      on the infectivity of Guignardia bidwellii on grape leaves",
      Phytopathology 67: 1378-1381.
    """
    if not days:
        return RuleResult(level="low", risk_index=0)

    infection_events = 0
    max_excess_hours = 0
    near_miss_hours = 0

    for d in days:
        if d.mean_temp_during_wet_hours is None or d.longest_continuous_wet_run == 0:
            continue

        req_hours = required_wet_hours(d.mean_temp_during_wet_hours)
        if d.longest_continuous_wet_run >= req_hours:
            infection_events += 1
            excess = d.longest_continuous_wet_run - req_hours
            if excess > max_excess_hours:
                max_excess_hours = excess
        else:
            miss = req_hours - d.longest_continuous_wet_run
            if miss <= NEAR_MISS_MARGIN:
                near_miss_hours = max(near_miss_hours, d.longest_continuous_wet_run)

    if infection_events >= MIN_EVENTS_HIGH or (
        infection_events == 1 and max_excess_hours >= EXCESS_HOURS_HIGH
    ):
        extra = min(25, (infection_events - 1) * 5 + max_excess_hours * 2)
        return RuleResult(level="high", risk_index=min(100, 75 + extra))

    if infection_events == 1:
        extra = min(34, max_excess_hours * 5)
        return RuleResult(level="medium", risk_index=40 + extra)

    if near_miss_hours > 0:
        return RuleResult(level="low", risk_index=min(39, 15 + near_miss_hours * 2))

    return RuleResult(level="low", risk_index=5)
