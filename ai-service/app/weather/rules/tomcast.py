from app.weather.daily_summary import DailySummary, RuleResult

HIGH_RISK_DSV_THRESHOLD = 15
MEDIUM_RISK_DSV_THRESHOLD = 7


def daily_dsv(mean_wet_temp_c: float | None, wet_hours: int) -> int:
    """Compute daily TOMCAST Disease Severity Value (DSV, 0 to 4).

    Published TOMCAST DSV lookup table (Pitblado, 1992; Bolkan & Reinert, 1994):
    - 13.0 <= T <= 17.0 C: 0-6h -> 0, 7-15h -> 1, 16-20h -> 2, 21+h -> 3
    - 17.0 <  T <= 20.0 C: 0-3h -> 0, 4-8h  -> 1, 9-15h  -> 2, 16-22h -> 3, 23+h -> 4
    - 20.0 <  T <= 25.0 C: 0-2h -> 0, 3-5h  -> 1, 6-12h  -> 2, 13-20h -> 3, 21+h -> 4
    - 25.0 <  T <= 29.0 C: 0-3h -> 0, 4-8h  -> 1, 9-15h  -> 2, 16-22h -> 3, 23+h -> 4
    - T < 13.0 C or T > 29.0 C: DSV = 0
    """
    if mean_wet_temp_c is None or wet_hours <= 0:
        return 0
    if mean_wet_temp_c < 13.0 or mean_wet_temp_c > 29.0:
        return 0

    if mean_wet_temp_c <= 17.0:
        if wet_hours <= 6:
            return 0
        if wet_hours <= 15:
            return 1
        if wet_hours <= 20:
            return 2
        return 3

    if mean_wet_temp_c <= 20.0 or mean_wet_temp_c > 25.0:
        if wet_hours <= 3:
            return 0
        if wet_hours <= 8:
            return 1
        if wet_hours <= 15:
            return 2
        if wet_hours <= 22:
            return 3
        return 4

    # 20.0 < mean_wet_temp_c <= 25.0
    if wet_hours <= 2:
        return 0
    if wet_hours <= 5:
        return 1
    if wet_hours <= 12:
        return 2
    if wet_hours <= 20:
        return 3
    return 4


def assess(days: list[DailySummary]) -> RuleResult:
    """Tomato early blight and Septoria leaf spot risk with TOMCAST.

    TOMCAST (TOMato disease foreCASTer) turns daily leaf wetness duration
    (approximated by hours with RH >= 90%) and mean temperature during wet hours
    into a daily Disease Severity Value (DSV, 0 to 4) summed over the 14-day window.

    Thresholds over the 14-day evaluation window:
    - cumulative DSV >= 15: high risk (first spray / action threshold reached)
    - cumulative DSV 7..14: medium risk (moderate disease pressure building)
    - cumulative DSV 0..6:  low risk

    Sources:
    - Pitblado, R. E. (1992), "The Development and Implementation of TOM-CAST",
      Ontario Ministry of Agriculture and Food, Ridgetown College.
    - Bolkan, H. A. and Reinert, W. R. (1994), "Developing and implementing
      tomato disease forecasting systems in California: TOM-CAST",
      Plant Disease 78(6): 594-598.
      https://www.apsnet.org/publications/plantdisease/backissues/Documents/1994Articles/PlantDisease78n06_545.PDF
    """
    if not days:
        return RuleResult(level="low", risk_index=0)

    total_dsv = sum(daily_dsv(d.mean_temp_during_wet_hours, d.wet_hours_count) for d in days)

    if total_dsv >= HIGH_RISK_DSV_THRESHOLD:
        scaled = 75 + min(25, (total_dsv - HIGH_RISK_DSV_THRESHOLD) * 2)
        return RuleResult(level="high", risk_index=min(100, scaled))

    if total_dsv >= MEDIUM_RISK_DSV_THRESHOLD:
        scaled = 40 + int(
            (total_dsv - MEDIUM_RISK_DSV_THRESHOLD)
            * 30
            / (HIGH_RISK_DSV_THRESHOLD - MEDIUM_RISK_DSV_THRESHOLD)
        )
        return RuleResult(level="medium", risk_index=scaled)

    scaled = int(total_dsv * 30 / MEDIUM_RISK_DSV_THRESHOLD)
    return RuleResult(level="low", risk_index=scaled)
