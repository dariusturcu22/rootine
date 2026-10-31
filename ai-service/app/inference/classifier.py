import csv
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np
import onnxruntime as ort


@dataclass(frozen=True)
class Label:
    class_id: int
    demo1_id: int
    crop: str
    disease: str
    eppo_code: str
    weather_rule: str
    source_dataset: str
    license: str

    @property
    def label(self) -> str:
        return f"{self.crop}_{self.disease}"


@dataclass(frozen=True)
class CropConfig:
    enabled: bool
    diagnosis_supported: bool
    support_level: str
    weather_rules: tuple[str, ...]


def load_labels(path: Path) -> list[Label]:
    with path.open(newline="", encoding="utf-8") as f:
        return [
            Label(
                class_id=int(row["class_id"]),
                demo1_id=int(row["demo1_id"]),
                crop=row["crop"],
                disease=row["disease"],
                eppo_code=row["eppo_code"],
                weather_rule=row["weather_rule"],
                source_dataset=row["source_dataset"],
                license=row["license"],
            )
            for row in csv.DictReader(f)
        ]


def load_crops(path: Path) -> dict[str, CropConfig]:
    data = json.loads(path.read_text(encoding="utf-8"))
    return {
        name: CropConfig(
            enabled=cfg["enabled"],
            diagnosis_supported=cfg["diagnosis_supported"],
            support_level=cfg["support_level"],
            weather_rules=tuple(cfg["weather_rules"]),
        )
        for name, cfg in data["crops"].items()
    }


def load_metadata(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


class Classifier:
    """ONNX Runtime wrapper. Real inference is not implemented yet (mock mode is used)."""

    def __init__(self, model_path: Path, metadata: dict[str, Any]) -> None:
        self.metadata = metadata
        self._session = ort.InferenceSession(str(model_path), providers=["CPUExecutionProvider"])

    def predict(self, tensor: np.ndarray, crop: str | None) -> list[tuple[int, float]]:
        raise NotImplementedError("Real inference is not implemented yet")
