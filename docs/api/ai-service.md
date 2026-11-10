# AI service API

First draft. Only the backend calls this service. It is not reachable from the browser.

Base URL inside Docker: `http://ai-service:8000`. Locally: `http://localhost:8000`.

While there is no `model.onnx`, or when `AI_MOCK=true`, the service runs in **mock mode** and returns the fixed responses shown below.

## GET /health

Response `200`:

```json
{ "status": "ok", "model_version": "plant-v1" }
GET /v1/cropsLists the crops that are enabled. Data comes from ai-service/models/plant-v1/labels.csv and crops.json.Fields of each crop:FieldMeaningcropCrop name, lowercase (tomato, potato)diagnosis_supportedtrue if photo diagnosis works for this cropweather_supportedtrue if at least one weather rule exists for this cropsupport_levellab_and_field_tested, lab_only or not_supportedweather_rulesNames of the weather rules used (hutton, tomcast)classesDisease names the model can output for this crop (healthy is a class)Response 200:JSON[
  {
    "crop": "potato",
    "diagnosis_supported": true,
    "weather_supported": true,
    "support_level": "lab_only",
    "weather_rules": ["hutton"],
    "classes": ["early_blight", "healthy", "late_blight"]
  },
  {
    "crop": "tomato",
    "diagnosis_supported": true,
    "weather_supported": true,
    "support_level": "lab_only",
    "weather_rules": ["hutton", "tomcast"],
    "classes": [
      "bacterial_spot", "early_blight", "healthy", "late_blight", "leaf_mold",
      "mosaic_virus", "septoria_leaf_spot", "spider_mites", "target_spot",
      "yellow_leaf_curl_virus"
    ]
  }
]
POST /v1/predictRequest: multipart/form-dataPartRequiredMeaningimageyesThe leaf photocropnoThe crop of the parcel. Without it the prediction is not maskedResponse 200:FieldValuesstatusconfident, unsure, invalid_image, crop_not_supportedcropThe crop used. Without crop in the request, the crop of the best prediction (mock: tomato)crop_checkok, mismatch, or null when no check was possible (invalid_image, crop_not_supported)top_predictions3 items: label (<crop>_<disease>), disease, probability (0 to 1). Empty list when status is invalid_image or crop_not_supportedsupport_levellab_and_field_tested, lab_only, not_supportedmodel_versionFor example plant-v1Example (status = confident, request with crop=potato):JSON{
  "status": "confident",
  "crop": "potato",
  "crop_check": "ok",
  "top_predictions": [
    { "label": "potato_early_blight", "disease": "early_blight", "probability": 0.92 },
    { "label": "potato_late_blight", "disease": "late_blight", "probability": 0.05 },
    { "label": "potato_healthy", "disease": "healthy", "probability": 0.03 }
  ],
  "support_level": "lab_only",
  "model_version": "plant-v1"
}
Example (crop=banana):JSON{
  "status": "crop_not_supported",
  "crop": "banana",
  "crop_check": null,
  "top_predictions": [],
  "support_level": "not_supported",
  "model_version": "plant-v1"
}
POST /v1/riskWeather-based disease risk for one location and crop. The AI service fetches 14 days of hourly weather (7 past + 7 forecast, in Europe/Bucharest time) from Open-Meteo with a 1-hour cache and retries transient failures.Implemented rule models:hutton (Hutton Criteria): Evaluates late_blight (Phytophthora infestans) risk on both potato and tomato (two consecutive days with min_temp_c >= 10 C and at least 6 hours of RH >= 90%, including continuous wet periods crossing midnight). Because P. infestans requires identical temperature and humidity thresholds on potato and tomato foliage, Hutton Criteria apply to both crops.tomcast (TOMCAST): Evaluates early_blight and septoria_leaf_spot risk on tomato using daily Disease Severity Values (DSV 0-4) accumulated over the 14-day window.Request (JSON):JSON{ "latitude": 46.77, "longitude": 23.59, "crop": "potato", "replay_date": "2026-06-20" }
replay_date is optional. When set, the risk is computed as if that day was today using the Open-Meteo Archive API.Validation: latitude from -90 to 90, longitude from -180 to 180. A bad value returns 422.Response 200:FieldMeaningcropThe crop from the requestlevellow, medium, high or not_modelledrisk_index0 to 100, display scalereasonPlain text explanationdiseasesList of disease, model (rule name), level, risk_indexgenerated_atISO 8601 UTC timeExample (mock):JSON{
  "crop": "potato",
  "level": "medium",
  "risk_index": 42,
  "reason": "Mock response: fixed medium risk, real weather rules are not implemented yet.",
  "diseases": [
    { "disease": "late_blight", "model": "hutton", "level": "medium", "risk_index": 42 }
  ],
  "generated_at": "2026-11-01T06:00:00Z"
}
For a crop without a weather model, level is not_modelled, risk_index is 0 and diseases is empty.POST /v1/threatLater. The mock returns a fixed answer:JSON{ "level": "low", "reason": "Mock response: threat level is not implemented yet." }
