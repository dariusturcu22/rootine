package app.aiclient;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HealthResponse(String status, @JsonProperty("model_version") String modelVersion) {}
