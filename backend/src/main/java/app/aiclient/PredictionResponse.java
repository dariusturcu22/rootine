package app.aiclient;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PredictionResponse(
    String status,
    String crop,
    @JsonProperty("crop_check") String cropCheck,
    @JsonProperty("top_predictions") List<Prediction> topPredictions,
    @JsonProperty("support_level") String supportLevel,
    @JsonProperty("model_version") String modelVersion) {}
