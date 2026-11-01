package app.diagnosis.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Whether the photo's crop matched the parcel's crop. {@code null} in the response when no check
 * was possible (for {@code invalid_image} or {@code crop_not_supported}).
 */
public enum CropCheck {
  OK("ok"),
  MISMATCH("mismatch");

  private final String jsonValue;

  CropCheck(String jsonValue) {
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
