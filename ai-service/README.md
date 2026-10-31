# AI service

Internal service of Rootine. It diagnoses plant diseases from leaf photos and computes weather-based disease risk. Only the backend calls it.

Stack: Python 3.11, FastAPI, uvicorn, ONNX Runtime, numpy, Pillow, httpx, pydantic-settings. Dev: pytest, respx, ruff. Training only (separate file): torch, torchvision, pandas, scikit-learn and others.

Without `model.onnx`, or with `AI_MOCK=true`, it runs in mock mode and returns the fixed responses described in [docs/api/ai-service.md](../docs/api/ai-service.md).

## Structure

```
app/
  main.py        loads labels, crops and metadata at startup; mock mode if no model file
  config.py      settings from env (AI_MOCK, MODEL_DIR, OPEN_METEO_URL, CACHE_TTL)
  schemas.py
  api/           predict, risk, threat, crops, health
  inference/     classifier.py, preprocess.py
  weather/       open_meteo_client, daily_summary, registry, rules/ (hutton, tomcast)
  threat_level.py
models/plant-v1/ labels.csv, crops.json, metadata.json (model.onnx is not in Git)
training/        notebooks, split.py, train.py, evaluate.py, export_onnx.py
scripts/         download_model.py
tests/
data/            gitignored
```

## Class names

`models/plant-v1/labels.csv` has the 13 tomato and potato classes. Class names come from the folder names of the official [PlantVillage dataset](https://github.com/spMohanty/PlantVillage-Dataset) (`Crop___Disease`). The mapping to our names is: crop and disease in lowercase with underscores. Three disease names are shortened: `Spider_mites Two-spotted_spider_mite` is `spider_mites`, `Tomato_Yellow_Leaf_Curl_Virus` is `yellow_leaf_curl_virus` and `Tomato_mosaic_virus` is `mosaic_virus`. `class_id` is the position in the sorted list of all 38 PlantVillage classes; `demo1_id` is the position among the 13 classes of Demo 1.

## Setup, run and test

See the [AI section of SETUP.md](../SETUP.md#ai-service). Rules for working here: [AGENTS.md](AGENTS.md).
