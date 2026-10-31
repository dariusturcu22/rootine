from app.weather.daily_summary import DailySummary, RuleResult


def assess(days: list[DailySummary]) -> RuleResult:
    """Potato late blight warning with the Hutton criteria.

    Rule: two consecutive days, each with a minimum temperature of at least 10 C
    and at least 6 hours with relative humidity of 90% or more.

    Source: Dancey, S. (2018), "Development and implementation of a new national
    warning system for potato late blight in Great Britain (Hutton Criteria)",
    James Hutton Institute / AHDB Potatoes.
    https://archive.ahdb.org.uk/development-and-implementation-of-a-new-national-warning-system-for-potato-late-blight-in-great-britain-hutton-criteria
    """
    raise NotImplementedError("Hutton criteria are not implemented yet")
