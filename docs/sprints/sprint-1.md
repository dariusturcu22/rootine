# Sprint 1

- **Dates:** 2026-10-28 - 2026-11-11
- **Goal:** Deliver the end-to-end Demo 1 vertical slice with real 13-class Potato and Tomato leaf diagnosis (MobileNetV3-Large ONNX) and live Open-Meteo weather disease risk rules (Hutton Criteria and TOMCAST).

## Planned

| Issue | Team | Owner | Status |
|---|---|---|---|
| #11 (ML-2 / ML-3) | AI | Andreea, Maia | Done |
| #59 (ML-7) | AI | Andreea | Done |
| #60 (ML-8) | AI | Andreea | Done |
| #61 (ML-9) | AI | Darius, Maia | Done |
| #62 (ML-10) | AI | Maia | Done |
| #76 (ML-11) | AI | Maia, Andreea | Done |

## Done

- **Dataset preparation and leak-free splitting (`prepare_plantvillage.py`, `check_split_leakage.py`):** Consolidated the 38-class PlantVillage catalog (`labels.csv`) and 14-crop configuration (`crops.json`) under `ai-service/models/plant-v1/` with CC BY-SA 3.0 attribution (Mohanty et al., 2016). Verified deterministic 70/15/15 stratified and dHash duplicate-grouped splits (`14,102` train / `3,146` val / `3,064` test images across the 13 Demo 1 Potato and Tomato classes) with zero cross-split leakage.
- **Model training and ONNX export (`train.py`, `v0.1.0-demo1`):** Trained `mobilenet_v3_large` with inverse-frequency class-weighted `CrossEntropyLoss` (2 frozen-backbone warmup epochs followed by full fine-tuning with CosineAnnealingLR). Exported `model.onnx` (opset 17, dynamic batch, SHA-256 `a26fd1e2ebc956d5828d1b2b22aa7e3c859876ec4d6ae4be04f2b419445acca4`) published to GitHub Release `v0.1.0-demo1`.
- **Real inference and fail-fast startup (`/v1/predict`, `/health`):** Replaced the 501 stub with real ONNX Runtime CPU inference (`run_in_threadpool`), EXIF orientation normalization, decompression-bomb and 10 MB upload guards, crop logit masking (ADR 0002), summed unmasked crop probability check (`crop_match_threshold = 0.50`), and confidence thresholding (`confidence_threshold = 0.60`). Added `mock: bool` to `/health` and fail-fast startup when `AI_MOCK=false` and `model.onnx` is missing.
- **Weather disease risk rules (`/v1/risk`, `hutton.py`, `tomcast.py`):** Implemented the Hutton Criteria for `late_blight` on potato and tomato (2 consecutive days with `min_temp_c >= 10.0 C` and `hours_rh_ge_90 >= 6`) and the TOMCAST Disease Severity Value (DSV 0-4) lookup table for tomato `early_blight` and `septoria_leaf_spot`. Hardened `OpenMeteoClient` with exponential-backoff retries on transient 5xx/network errors and `summarize_days` with cross-midnight wet-period carry-over and missing-value (`None`) handling.

## Not done and why

- All planned Sprint 1 AI tasks for Demo 1 were completed. Calibrating a post-hoc validation temperature scaling factor (`temperature` in `metadata.json`, currently `1.0`) and expanding field-photo background augmentation are scheduled for Sprint 2.

## AI Service: Training & Hardware Benchmark

Benchmark recorded on Google Colab (NVIDIA Tesla T4 GPU, 16 GB VRAM, mixed precision `fp16` enabled) for training and local CPU (`onnxruntime` `CPUExecutionProvider`, single-image batch `1x3x224x224`, 50-run average after 5 warmup runs) for inference:

| Configuration | Classes | Split Sizes (train / val / test) | T4 Time / Epoch (s) | Total Train Time (7 epochs) | DataLoader Wait (`data_wait_pct`) | Peak VRAM (`peak_mem_gb`) | ONNX CPU Latency (ms/image) | ONNX vs PyTorch Max Diff |
|---|---|---|---|---|---|---|---|---|
| `demo1_13c` (Potato & Tomato) | 13 | 14,102 / 3,146 / 3,064 | 26.4 s | 184.8 s | 12% | 0.68 GB | 3.77 ms | 1.34e-05 |
| `all_38c` (Full PlantVillage baseline) | 38 | 38,015 / 8,142 / 8,148 | 68.5 s | 68.5 s (1-epoch check) | 14% | 0.71 GB | 3.85 ms | < 1.00e-04 |

## AI Service: Model Evaluation Summary (`plant-v1`)

- **Architecture:** `mobilenet_v3_large` (`input_size = 224`, ImageNet normalization `mean = [0.485, 0.456, 0.406]`, `std = [0.229, 0.224, 0.225]`)
- **Best Checkpoint:** Epoch 7 of 7 (early stopping on validation macro F1)
- **Test Accuracy:** `99.67%` (`0.996736` on `3,064` held-out test images)
- **Test Macro F1:** `99.67%` (`0.996740` across all 13 classes)
- **Confidence & Crop Check Thresholds:**
  - `confidence_threshold = 0.60`: predictions with top-1 masked softmax probability `< 0.60` return `status = "unsure"`.
  - `crop_match_threshold = 0.50`: when a `crop` is provided, if the sum of unmasked softmax probabilities across that crop's classes is `< 0.50`, `crop_check` returns `"mismatch"`.

### Per-Class Test Metrics (3,064 Images)

| Class | Precision | Recall | F1 | Support |
|---|---|---|---|---|
| Potato___Early_blight | 1.000 | 1.000 | 1.000 | 148 |
| Potato___healthy | 1.000 | 1.000 | 1.000 | 32 |
| Potato___Late_blight | 0.993 | 1.000 | 0.997 | 144 |
| Tomato___Bacterial_spot | 0.983 | 1.000 | 0.991 | 343 |
| Tomato___Early_blight | 0.985 | 0.993 | 0.989 | 136 |
| Tomato___healthy | 1.000 | 0.996 | 0.998 | 250 |
| Tomato___Late_blight | 1.000 | 1.000 | 1.000 | 285 |
| Tomato___Leaf_Mold | 1.000 | 1.000 | 1.000 | 124 |
| Tomato___Tomato_mosaic_virus | 1.000 | 1.000 | 1.000 | 48 |
| Tomato___Septoria_leaf_spot | 1.000 | 0.978 | 0.989 | 279 |
| Tomato___Spider_mites Two-spotted_spider_mite | 1.000 | 0.997 | 0.998 | 296 |
| Tomato___Target_Spot | 0.995 | 0.995 | 0.995 | 213 |
| Tomato___Tomato_Yellow_Leaf_Curl_Virus | 1.000 | 1.000 | 1.000 | 766 |

## Retrospective

- **What went well:**
  - Sharing `app.inference.preprocess.preprocess_image` directly inside `training/train.py` guaranteed 100% preprocessing parity between PyTorch training and ONNX Runtime inference (`max_diff = 1.34e-05`).
  - Inverse-frequency class weighting allowed minority classes (`Potato___healthy` with 32 test images, `Tomato___Tomato_mosaic_virus` with 48 test images) to reach `1.000` F1 without overfitting majority classes (`Tomato___Tomato_Yellow_Leaf_Curl_Virus` with 766 test images).
  - Unit and endpoint tests caught subtle weather edge cases early (carrying continuous wet hours across midnight `23:00 -> 00:00` without dropping carried hours when a second wet spell occurs later the same day, and breaking wet streaks when hourly temperature is `None`).
- **What to improve (Challenges):**
  - **Studio / Background Bias on out-of-distribution field photos:** While the model achieves `99.67%` accuracy on held-out PlantVillage test images, Grad-CAM visual explanations on external web/garden photos revealed shortcut learning on studio background colors and non-leaf artifacts (for example, blue paper backgrounds triggering `Tomato___Leaf_Mold` with 88% confidence, or human fingers/soil in frame skewing predictions toward `Early_blight`). Tightly cropping the image around the leaf lamina eliminates the background distraction and restores high-confidence correct predictions (`99%` on `Leaf_Mold`), which validates marking PlantVillage-only crops as `support_level: "lab_only"`.
  - **Class imbalance in raw PlantVillage:** Several crops in the 38-class catalog only have a single class (`healthy` or a single disease), requiring explicit `diagnosis_supported: false` and `support_level: "not_supported"` guards in `crops.json`.
- **Actions for the next sprint:**
  - Mitigate background shortcut learning in Sprint 2 by compositing segmented PlantVillage leaves over diverse natural/random backgrounds and adding stronger color/background augmentations during training.
  - Add UI framing guidance encouraging users to center and tightly crop the symptomatic leaf before uploading, and calibrate post-hoc temperature scaling (`temperature` in `metadata.json`) on out-of-distribution validation samples.
