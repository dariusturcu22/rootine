from app.weather.daily_summary import DailySummary, RuleResult

MIN_INFECT_TEMP_C = 2.0
MAX_INFECT_TEMP_C = 26.0
NEAR_MIN_TEMP_C = 6.0

SEVERITY_NONE = 0
SEVERITY_LIGHT = 1
SEVERITY_MODERATE = 2
SEVERITY_HEAVY = 3

MIN_MOD_OR_HEAVY_DAYS_FOR_HIGH = 2
MIN_TOTAL_SEV_FOR_HIGH = 4

NEAR_WET_HOURS_MED_LOW = 6
NEAR_WET_HOURS_MIN_LOW = 3

# Revised Mills & LaPlante (1954) / MacHardy & Gadoury (1989) table for
# Venturia inaequalis (apple scab) ascospore primary infection.
# Each tuple is: (min_temp_c, max_temp_c, light_hours, moderate_hours, heavy_hours)
MILLS_TABLE: tuple[tuple[float, float, int, int, int], ...] = (
    (2.0, 5.0, 30, 40, 60),
    (5.0, 6.0, 21, 30, 45),
    (6.0, 8.0, 15, 20, 30),
    (8.0, 10.0, 12, 16, 24),
    (10.0, 12.0, 11, 14, 21),
    (12.0, 14.0, 10, 13, 19),
    (14.0, 16.0, 9, 12, 18),
    (16.0, 24.0, 9, 12, 18),
    (24.0, 26.0, 11, 14, 21),
)


def daily_mills_severity(mean_wet_temp_c: float | None, continuous_wet_hours: int) -> int:
    """Return daily Mills apple scab infection severity (0=none, 1=light, 2=moderate, 3=heavy).

    Evaluates the longest continuous wet run (hours with RH >= 90%) and the mean
    temperature during wet hours against the published Mills Table thresholds:
    - 6.0 <= T <= 8.0 C:   >= 15h light (1), >= 20h moderate (2), >= 30h heavy (3)
    - 16.0 <= T <= 24.0 C: >= 9h light (1),  >= 12h moderate (2), >= 18h heavy (3)
    - T < 2.0 C or T > 26.0 C: no infection (0)
    """
    if mean_wet_temp_c is None or continuous_wet_hours <= 0:
        return SEVERITY_NONE
    if mean_wet_temp_c < MIN_INFECT_TEMP_C or mean_wet_temp_c > MAX_INFECT_TEMP_C:
        return SEVERITY_NONE

    for idx, (t_min, t_max, light_h, mod_h, heavy_h) in enumerate(MILLS_TABLE):
        in_band = (
            t_min <= mean_wet_temp_c <= t_max if idx == 0 else t_min < mean_wet_temp_c <= t_max
        )
        if in_band:
            if continuous_wet_hours >= heavy_h:
                return SEVERITY_HEAVY
            if continuous_wet_hours >= mod_h:
                return SEVERITY_MODERATE
            if continuous_wet_hours >= light_h:
                return SEVERITY_LIGHT
            return SEVERITY_NONE
    return SEVERITY_NONE


def assess(days: list[DailySummary]) -> RuleResult:
    """Apple scab (Venturia inaequalis) risk assessment using the Mills Table.

    Evaluates each day in the 14-day window using `mean_temp_during_wet_hours`
    and `longest_continuous_wet_run` (continuous hours at RH >= 90%).

    Mapping to risk level and risk_index (0..100):
    - High (75..100): at least one heavy infection event (severity 3), OR two or
      more moderate infection events (severity 2), OR cumulative severity >= 4.
    - Medium (40..74): one moderate infection event (severity 2) OR at least one
      light infection event (severity 1).
    - Low (0..39): no infection events met, scaled by proximity to the light
      infection wet-hour threshold on warm wet days.

    Sources:
    - Mills, W. D. (1944), "Efficient use of sulfur dusts and sprays during rain
      to control apple scab", Cornell Extension Bulletin 630.
    - MacHardy, W. E. and Gadoury, D. M. (1989), "A revision of Mills's criteria
      for predicting apple scab infection periods", Phytopathology 79: 304-310.
    """
    if not days:
        return RuleResult(level="low", risk_index=0)

    severities = [
        daily_mills_severity(d.mean_temp_during_wet_hours, d.longest_continuous_wet_run)
        for d in days
    ]
    peak_sev = max(severities)
    total_sev = sum(severities)
    mod_or_heavy_days = sum(1 for s in severities if s >= SEVERITY_MODERATE)

    if (
        peak_sev == SEVERITY_HEAVY
        or mod_or_heavy_days >= MIN_MOD_OR_HEAVY_DAYS_FOR_HIGH
        or total_sev >= MIN_TOTAL_SEV_FOR_HIGH
    ):
        extra = min(
            25,
            (total_sev - SEVERITY_HEAVY) * 5 + (10 if peak_sev == SEVERITY_HEAVY else 0),
        )
        return RuleResult(level="high", risk_index=min(100, 75 + max(0, extra)))

    if peak_sev >= SEVERITY_LIGHT:
        base = 55 if peak_sev == SEVERITY_MODERATE else 40
        extra = min(19, (total_sev - SEVERITY_LIGHT) * 8)
        return RuleResult(level="medium", risk_index=min(74, base + max(0, extra)))

    near_wet_hours = max(
        (
            d.longest_continuous_wet_run
            for d in days
            if d.mean_temp_during_wet_hours is not None
            and NEAR_MIN_TEMP_C <= d.mean_temp_during_wet_hours <= MAX_INFECT_TEMP_C
        ),
        default=0,
    )
    if near_wet_hours >= NEAR_WET_HOURS_MED_LOW:
        return RuleResult(level="low", risk_index=25)
    if near_wet_hours >= NEAR_WET_HOURS_MIN_LOW:
        return RuleResult(level="low", risk_index=15)

    return RuleResult(level="low", risk_index=5)
