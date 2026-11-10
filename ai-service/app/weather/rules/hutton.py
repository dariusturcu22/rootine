from app.weather.daily_summary import DailySummary, RuleResult

MIN_TEMP_THRESHOLD_C = 10.0
MIN_WET_HOURS_RH90 = 6
NEAR_WET_HOURS_RH90 = 4


def _is_hutton_day(day: DailySummary) -> bool:
    """Return True if a single day meets the Hutton criteria."""
    return day.min_temp_c >= MIN_TEMP_THRESHOLD_C and day.hours_rh_ge_90 >= MIN_WET_HOURS_RH90


def _longest_consecutive_hutton_streak(days: list[DailySummary]) -> int:
    """Count the longest streak of consecutive calendar days meeting Hutton criteria."""
    ordered = sorted(days, key=lambda d: d.day)
    longest = 0
    current = 0
    prev_day = None
    for summary in ordered:
        if _is_hutton_day(summary):
            if prev_day is not None and (summary.day - prev_day).days == 1:
                current += 1
            else:
                current = 1
            if current > longest:
                longest = current
            prev_day = summary.day
        else:
            current = 0
            prev_day = None
    return longest


def assess(days: list[DailySummary]) -> RuleResult:
    """Potato and tomato late blight warning with the Hutton criteria.

    Rule: two consecutive days, each with a minimum temperature of at least 10 C
    and at least 6 hours with relative humidity of 90% or more.

    Applicability to tomato: Late blight on both potato (Solanum tuberosum) and
    tomato (Solanum lycopersicum) is caused by the same oomycete pathogen,
    Phytophthora infestans. Because sporangial production and infection require
    the same temperature (>= 10 C) and high-humidity (>= 90% RH for >= 6 h)
    thresholds on both Solanaceae hosts, the Hutton Criteria are applied here
    for both potato and tomato late blight risk assessment.

    Source: Dancey, S. (2018), "Development and implementation of a new national
    warning system for potato late blight in Great Britain (Hutton Criteria)",
    James Hutton Institute / AHDB Potatoes.
    https://archive.ahdb.org.uk/development-and-implementation-of-a-new-national-warning-system-for-potato-late-blight-in-great-britain-hutton-criteria
    """
    if not days:
        return RuleResult(level="low", risk_index=0)

    streak = _longest_consecutive_hutton_streak(days)
    hutton_days_count = sum(1 for d in days if _is_hutton_day(d))

    if streak >= 2:
        extra = min(25, (streak - 2) * 10 + max(0, hutton_days_count - 2) * 5)
        return RuleResult(level="high", risk_index=min(100, 75 + extra))

    if hutton_days_count == 1:
        return RuleResult(level="medium", risk_index=50)

    near_days = sum(
        1
        for d in days
        if d.min_temp_c >= MIN_TEMP_THRESHOLD_C and d.hours_rh_ge_90 >= NEAR_WET_HOURS_RH90
    )
    if near_days >= 2:
        return RuleResult(level="medium", risk_index=40)
    if near_days == 1:
        return RuleResult(level="low", risk_index=20)

    return RuleResult(level="low", risk_index=10)
