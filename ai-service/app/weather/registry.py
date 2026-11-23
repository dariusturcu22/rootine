from collections.abc import Callable

from app.weather.daily_summary import DailySummary, RuleResult
from app.weather.rules import hutton, mills, spotts, tomcast

WeatherRule = Callable[[list[DailySummary]], RuleResult]

# Names here must match the weather_rule column in labels.csv and crops.json.
WEATHER_RULES: dict[str, WeatherRule] = {
    "hutton": hutton.assess,
    "mills": mills.assess,
    "spotts": spotts.assess,
    "tomcast": tomcast.assess,
}
