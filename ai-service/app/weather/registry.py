from collections.abc import Callable

from app.weather.daily_summary import DailySummary, RuleResult
from app.weather.rules import hutton, mills, tomcast

WeatherRule = Callable[[list[DailySummary]], RuleResult]

# Names here must match the weather_rule column in labels.csv and crops.json.
WEATHER_RULES: dict[str, WeatherRule] = {
    "hutton": hutton.assess,
    "mills": mills.assess,
    "tomcast": tomcast.assess,
}
