package app.diagnosis;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The outcome of a diagnosis. The same values the AI service returns. {@code invalid_image} and
 * {@code crop_not_supported} are valid results, not errors.
 */
public enum DiagnosisStatus {
  CONFIDENT("confident"),
  UNSURE("unsure"),
  INVALID_IMAGE("invalid_image"),
  CROP_NOT_SUPPORTED("crop_not_supported");

  private final String jsonValue;

  DiagnosisStatus(String jsonValue) {
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
