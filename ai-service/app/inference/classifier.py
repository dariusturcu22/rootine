import csv
import json
from collections.abc import Sequence
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Literal, Protocol

import numpy as np
import onnxruntime as ort

from app.config import Settings
from app.schemas import Prediction

TOP_PREDICTION_COUNT = 3


@dataclass(frozen=True)
class Label:
    class_id: int
    demo1_id: int | None
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
                demo1_id=int(row["demo1_id"]) if row["demo1_id"] else None,
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


class InferenceSession(Protocol):
    def get_inputs(self) -> Sequence[Any]: ...

    def run(
        self, output_names: list[str] | None, input_feed: dict[str, np.ndarray]
    ) -> list[np.ndarray]: ...


@dataclass(frozen=True)
class Classification:
    status: Literal["confident", "unsure"]
    crop: str
    crop_check: Literal["ok", "mismatch"]
    top_predictions: list[Prediction]


def create_inference_session(model_path: Path) -> InferenceSession:
    return ort.InferenceSession(str(model_path), providers=["CPUExecutionProvider"])


def softmax(logits: np.ndarray) -> np.ndarray:
    exponentials = np.exp(logits - np.max(logits))
    return exponentials / exponentials.sum()


class Classifier:
    """Runs the ONNX model and turns its logits into a masked, ranked diagnosis."""

    def __init__(
        self,
        session: InferenceSession,
        labels: list[Label],
        metadata: dict[str, Any],
        settings: Settings,
    ) -> None:
        self._session = session
        self._input_name = session.get_inputs()[0].name
        self._classes = sorted(
            (label for label in labels if label.demo1_id is not None),
            key=lambda label: label.demo1_id,
        )
        self._input_size = metadata["input_size"]
        self._temperature = metadata["temperature"]
        self._confidence_threshold = metadata.get(
            "confidence_threshold", settings.default_confidence_threshold
        )
        self._crop_match_threshold = settings.crop_match_threshold
        self._check_output_size()

    def has_classes_for(self, crop: str) -> bool:
        return any(label.crop == crop for label in self._classes)

    def predict(self, tensor: np.ndarray, crop: str | None) -> Classification:
        logits = self._run_model(tensor) / self._temperature
        probabilities = softmax(logits)
        if crop is None:
            top_crop = self._classes[int(np.argmax(probabilities))].crop
            all_indexes = list(range(len(self._classes)))
            return self._classify(top_crop, "ok", all_indexes, probabilities)
        crop_indexes = [index for index, label in enumerate(self._classes) if label.crop == crop]
        crop_score = float(probabilities[crop_indexes].sum())
        crop_check = "ok" if crop_score >= self._crop_match_threshold else "mismatch"
        return self._classify(crop, crop_check, crop_indexes, softmax(logits[crop_indexes]))

    def _classify(
        self,
        crop: str,
        crop_check: Literal["ok", "mismatch"],
        class_indexes: list[int],
        probabilities: np.ndarray,
    ) -> Classification:
        ranking = np.argsort(probabilities)[::-1][:TOP_PREDICTION_COUNT]
        top_predictions = [
            Prediction(
                label=self._classes[class_indexes[position]].label,
                disease=self._classes[class_indexes[position]].disease,
                probability=float(probabilities[position]),
            )
            for position in ranking
        ]
        is_confident = top_predictions[0].probability >= self._confidence_threshold
        status = "confident" if is_confident else "unsure"
        return Classification(status, crop, crop_check, top_predictions)

    def _run_model(self, tensor: np.ndarray) -> np.ndarray:
        outputs = self._session.run(None, {self._input_name: tensor})
        return np.asarray(outputs[0][0], dtype=np.float64)

    def _check_output_size(self) -> None:
        probe = np.zeros((1, 3, self._input_size, self._input_size), dtype=np.float32)
        output_size = self._run_model(probe).shape[-1]
        if output_size != len(self._classes):
            raise RuntimeError(
                f"The model returns {output_size} classes, but labels.csv has "
                f"{len(self._classes)} classes with a demo1_id. Refusing to serve a wrong model."
            )
