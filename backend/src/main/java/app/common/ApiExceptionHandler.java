package app.common;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns application exceptions into RFC 9457 Problem Details responses. Spring serializes {@link
 * ProblemDetail} as {@code application/problem+json}, which is the error shape the API contract
 * promises (fields {@code type}, {@code title}, {@code status} and {@code detail}).
 */
@Hidden
@RestControllerAdvice
public class ApiExceptionHandler {

  /**
   * Returns 501 (Not Implemented) so callers can see the endpoint exists while the logic is still
   * missing.
   */
  @ExceptionHandler(NotImplementedException.class)
  public ProblemDetail handleNotImplemented(NotImplementedException exception) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, exception.getMessage());
    problemDetail.setTitle("Not Implemented");
    return problemDetail;
  }
}
