package app.aiclient;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DiseaseRisk(
    String disease, String model, String level, @JsonProperty("risk_index") int riskIndex) {}
