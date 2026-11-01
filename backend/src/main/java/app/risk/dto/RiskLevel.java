package app.risk.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Weather-based disease risk level, as reported by the AI service. {@code not_modelled} means the
 * crop has no weather model.
 */
public enum RiskLevel {
  LOW("low"),
  MEDIUM("medium"),
  HIGH("high"),
  NOT_MODELLED("not_modelled");

  private final String jsonValue;

  RiskLevel(String jsonValue) {
    this.jsonValue = jsonValue;
  }

  @JsonValue
  public String jsonValue() {
    return jsonValue;
  }

  @Override
  public String toString() {
    return jsonValue;
  }
}
