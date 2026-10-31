# 0004 - ONNX Runtime for serving, no PyTorch

## Context
PyTorch makes the serving image very large and slow to build. Serving only needs to run a trained model.

## Decision
Train with PyTorch (code in `ai/training/`, separate `requirements-train.txt`). Export the model to ONNX. Serve with ONNX Runtime. `ai/requirements.txt` never contains torch.

## Consequences
- Smaller and faster Docker image for the AI service.
- Training and serving have separate dependencies.
- The model must be exportable to ONNX, and the preprocessing must match training exactly (values are in `metadata.json`).
