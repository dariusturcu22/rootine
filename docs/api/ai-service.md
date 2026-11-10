# AI service API

First draft. Only the backend calls this service. It is not reachable from the browser.

Base URL inside Docker: `http://ai-service:8000`. Locally: `http://localhost:8000`.

With `AI_MOCK=true` the service runs in **mock mode** and returns the fixed responses shown below. With `AI_MOCK=false` the service loads `models/plant-v1/model.onnx` and **refuses to start** if the file is missing (download it with `python scripts/download_model.py --tag v0.1.0-demo1`). Real weather rules are not implemented yet; with `AI_MOCK=false`, `risk` returns `501`.

## GET /health

Response `200`:

```json
{ "status": "ok", "model_version": "plant-v1", "mock": false }
```

`mock` is `true` when the service runs with `AI_MOCK=true` and returns fixed answers.

## GET /v1/crops

Lists the crops that are enabled. Data comes from `ai-service/models/plant-v1/labels.csv` and `crops.json`.

Fields of each crop:

| Field | Meaning |
|---|---|
| `crop` | Crop name, lowercase (`tomato`, `potato`) |
| `diagnosis_supported` | `true` if photo diagnosis works for this crop |
| `weather_supported` | `true` if at least one weather rule exists for this crop |
| `support_level` | `lab_and_field_tested`, `lab_only` or `not_supported` |
| `weather_rules` | Names of the weather rules used (`hutton`, `tomcast`) |
| `classes` | Disease names the model can output for this crop (`healthy` is a class) |

Response `200`:

```json
[
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
```

## POST /v1/predict

Request: `multipart/form-data`

| Part | Required | Meaning |
|---|---|---|
| `image` | yes | The leaf photo, at most 10 MB |
| `crop` | no | The crop of the parcel, compared without case and surrounding spaces. Without it the prediction is not masked |

Response `200`:

| Field | Values |
|---|---|
| `status` | `confident`, `unsure`, `invalid_image`, `crop_not_supported` |
| `crop` | The crop used. Without `crop` in the request, the crop of the best prediction (mock: `tomato`) |
| `crop_check` | `ok`, `mismatch`, or `null` when no check was possible (`invalid_image`, `crop_not_supported`) |
| `top_predictions` | Up to 3 items: `label` (`<crop>_<disease>`), `disease`, `probability` (0 to 1). Empty list when `status` is `invalid_image` or `crop_not_supported` |

How the answer is built:

- With `crop`, only the classes of that crop can win: the probabilities are recomputed over that crop's classes only.
- `crop_check` is `mismatch` when the classes of the requested crop together get less than 0.5 of the probability before masking (the photo does not look like that crop). It does not change `status`. Without `crop`, `crop_check` is `ok`.
- `status` is `unsure` when the best probability is below `confidence_threshold` from `metadata.json`, or below 0.60 when that key is missing.
- `invalid_image` is returned for a file that is not a readable image, a file larger than 10 MB, and an image with too many pixels (decompression bomb). The EXIF orientation of a photo is applied before resizing.
- A crop that is unknown, disabled, or without diagnosis support gives `crop_not_supported` before any inference.
| `support_level` | `lab_and_field_tested`, `lab_only`, `not_supported` |
| `model_version` | For example `plant-v1` |

Example (`status` = `confident`, request with `crop=potato`):

```json
{
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
```

Example (`crop=banana`):

```json
{
  "status": "crop_not_supported",
  "crop": "banana",
  "crop_check": null,
  "top_predictions": [],
  "support_level": "not_supported",
  "model_version": "plant-v1"
}
```

## POST /v1/risk

Weather-based disease risk for one location and crop. The AI service fetches the weather from Open-Meteo itself.

Request (JSON):

```json
{ "latitude": 46.77, "longitude": 23.59, "crop": "potato", "replay_date": "2026-06-20" }
```

`replay_date` is optional. When set, the risk is computed as if that day was today.

Validation: `latitude` from -90 to 90, `longitude` from -180 to 180. A bad value returns `422`.

Response `200`:

| Field | Meaning |
|---|---|
| `crop` | The crop from the request |
| `level` | `low`, `medium`, `high` or `not_modelled` |
| `risk_index` | 0 to 100, display scale |
| `reason` | Plain text explanation |
| `diseases` | List of `disease`, `model` (rule name), `level`, `risk_index` |
| `generated_at` | ISO 8601 UTC time |

Example (mock):

```json
{
  "crop": "potato",
  "level": "medium",
  "risk_index": 42,
  "reason": "Mock response: fixed medium risk, real weather rules are not implemented yet.",
  "diseases": [
    { "disease": "late_blight", "model": "hutton", "level": "medium", "risk_index": 42 }
  ],
  "generated_at": "2026-11-01T06:00:00Z"
}
```

For a crop without a weather model, `level` is `not_modelled`, `risk_index` is `0` and `diseases` is empty.

## POST /v1/threat

Later. The mock returns a fixed answer:

```json
{ "level": "low", "reason": "Mock response: threat level is not implemented yet." }
```
