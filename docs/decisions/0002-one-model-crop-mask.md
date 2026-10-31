# 0002 - One image model with a crop mask

## Context
The app must support many crops. One model per crop means many models to train, store, load and keep in sync. Farmers already know the crop of each parcel.

## Decision
One image model for all crops (MobileNetV3-Large). The parcel's crop is used as a mask: only the classes of that crop can win. Classes are read from `labels.csv` and `crops.json`, never hardcoded. Without a crop, the prediction is not masked.

## Consequences
- Adding a crop means adding rows to `labels.csv` and an entry in `crops.json`, then retraining. No code change.
- A photo of the wrong crop can be detected (`crop_check` = `mismatch`).
- One model file to ship and load.
- Demo 1 covers only tomato and potato (13 classes).
