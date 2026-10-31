from app.weather.daily_summary import DailySummary, RuleResult


def assess(days: list[DailySummary]) -> RuleResult:
    """Tomato early blight and Septoria leaf spot risk with TOMCAST.

    TOMCAST (TOMato disease foreCASTer) turns daily leaf wetness and temperature
    into a Disease Severity Value (DSV, 0 to 4) that is summed over time.

    Source: Pitblado, R. E. (Ontario, Canada); see also Bolkan, H. A. and
    Reinert, W. R. (1994). The AI team must add the exact DSV table and a link
    to the published reference here before implementing the function.
    """
    raise NotImplementedError("TOMCAST is not implemented yet")
