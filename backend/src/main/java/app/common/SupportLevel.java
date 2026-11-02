package app.common;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How well a crop or a diagnosis is supported, as reported by the AI service. The JSON values are
 * lowercase with underscores, matching the AI service and the API contract.
 */
public enum SupportLevel {
  LAB_AND_FIELD_TESTED("lab_and_field_tested"),
  LAB_ONLY("lab_only"),
  NOT_SUPPORTED("not_supported");

  private final String jsonValue;

  SupportLevel(String jsonValue) {
    this.jsonValue = jsonValue;
  }

  /**
   * The value used in JSON. Jackson serializes an enum with {@code toString} when the mapper is
   * configured for it; springdoc reads this value for the schema, so the generated types use the
   * contract's lowercase form.
   */
  @JsonValue
  public String jsonValue() {
    return jsonValue;
  }

  @Override
  public String toString() {
    return jsonValue;
  }
}
