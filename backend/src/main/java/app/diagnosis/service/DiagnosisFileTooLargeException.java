package app.diagnosis.service;

/** Signals that a diagnosis image exceeds the configured per-file size limit. */
public class DiagnosisFileTooLargeException extends RuntimeException {

  public DiagnosisFileTooLargeException(String message) {
    super(message);
  }
}
