# AGENTS.md - ai

Read the [root AGENTS.md](../AGENTS.md) first. Its rules apply here, especially: no attribution, English only, tests with every change, no API change without `docs/api/`.

## Stack

Python 3.11 (ruff target `py311`), FastAPI, uvicorn, onnxruntime, numpy, Pillow, httpx, pydantic-settings. Dev: pytest, respx, ruff. Training: torch and others in `training/requirements-train.txt` only.

## Structure

See [README.md](README.md).

## Commands

All commands are in the [AI section of SETUP.md](../SETUP.md#ai-service). Before a PR: `ruff check .`, `ruff format --check .`, `pytest`.

## Local rules

- No torch in `requirements.txt`. Serving uses ONNX Runtime only. Training code stays in `training/`.
- Never hardcode classes or crops. Read them from `models/plant-v1/labels.csv` and `crops.json`.
- Weather rules (`app/weather/rules/`) are pure functions with a test and a link to their published source in the docstring.
- Model files and datasets never go into Git (`*.onnx`, `*.pt`, `*.pth`, `data/`). Models are attached to GitHub Releases and fetched with `scripts/download_model.py`.
- Mock responses must follow `docs/api/ai-service.md` exactly. If you change an endpoint, update that file in the same PR.
- Golden-image tests that need `model.onnx` must be skipped when the file is missing (`pytest.mark.skipif`).
- Every dataset used must have its license in the `license` column of `labels.csv`. Fill `eppo_code` only from the EPPO Global Database, not from memory.
