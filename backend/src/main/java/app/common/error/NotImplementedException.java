package app.common.error;

/**
 * Thrown by a controller whose endpoint is published in the API contract but whose logic is not
 * implemented yet. The global exception handler turns it into an RFC 9457 Problem Details response
 * with status 501 (Not Implemented), so the endpoint never pretends to work.
 */
public class NotImplementedException extends RuntimeException {

  public NotImplementedException(String message) {
    super(message);
  }
}
