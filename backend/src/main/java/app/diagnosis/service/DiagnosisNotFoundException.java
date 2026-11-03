package app.diagnosis.service;

/** Signals that a diagnosis resource does not exist or is not owned by the current user. */
public class DiagnosisNotFoundException extends RuntimeException {

  public DiagnosisNotFoundException(String message) {
    super(message);
  }
}
