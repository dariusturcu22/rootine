package app.aiclient;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record RiskResponse(
    String crop,
    String level,
    @JsonProperty("risk_index") int riskIndex,
    String reason,
    List<DiseaseRisk> diseases,
    @JsonProperty("generated_at") Instant generatedAt) {}
