from dataclasses import dataclass

import numpy as np

DEMO_CLASS_COUNT = 13
POTATO_EARLY_BLIGHT = 0
POTATO_LATE_BLIGHT = 2
TOMATO_EARLY_BLIGHT = 4
STRONG_LOGIT = 8.0


@dataclass(frozen=True)
class FakeModelInput:
    name: str


class FakeSession:
    input_name = "pixels"

    def __init__(self, logits: list[float]) -> None:
        self.logits = np.asarray([logits], dtype=np.float32)
        self.received_input_names: list[str] = []

    def get_inputs(self) -> list[FakeModelInput]:
        return [FakeModelInput(name=self.input_name)]

    def run(
        self, output_names: list[str] | None, input_feed: dict[str, np.ndarray]
    ) -> list[np.ndarray]:
        self.received_input_names.extend(input_feed)
        return [self.logits]


def logits_favouring(class_index: int, class_count: int = DEMO_CLASS_COUNT) -> list[float]:
    logits = [0.0] * class_count
    logits[class_index] = STRONG_LOGIT
    return logits
