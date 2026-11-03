package app.diagnosis.service;

/** Signals that an uploaded diagnosis image is missing, too large, or not a supported image. */
public class InvalidDiagnosisUploadException extends RuntimeException {

  public InvalidDiagnosisUploadException(String message) {
    super(message);
  }

  public InvalidDiagnosisUploadException(String message, Throwable cause) {
    super(message, cause);
  }
}
