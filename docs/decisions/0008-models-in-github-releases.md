# 0008 - Models in GitHub Releases, datasets outside Git

## Context
Trained models and datasets are large binary files. Git is bad at them, and a public repository must not contain data we may not redistribute.

## Decision
No datasets and no model files in Git (`*.onnx`, `*.pt`, `*.pth` and `ai-service/data/` are ignored). A trained `model.onnx` is attached to a GitHub Release. `ai-service/scripts/download_model.py --tag <tag>` downloads it into `ai-service/models/plant-v1/`. Small text files that describe the model (`labels.csv`, `crops.json`, `metadata.json`) are in Git.

## Consequences
- The repository stays small and clean.
- The AI service runs in mock mode until a model is downloaded.
- Every model version is tied to a release tag, so it can be traced and reproduced.
- Each dataset's license must be written in `labels.csv` before its data is used.
