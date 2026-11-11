package app.common.error;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns application exceptions into RFC 9457 Problem Details responses. Spring serializes {@link
 * ProblemDetail} as {@code application/problem+json}, which is the error shape the API contract
 * promises (fields {@code type}, {@code title}, {@code status} and {@code detail}).
 *
 * <p>{@link Hidden} keeps this advice out of the generated OpenAPI document; the error responses
 * are declared explicitly on each endpoint instead.
 */
@Hidden
@RestControllerAdvice
public class ApiExceptionHandler {

  /**
   * Handles endpoints that are published in the contract but not implemented yet. Returns 501 (Not
   * Implemented) so callers can see the endpoint exists while the logic is still missing.
   */
  @ExceptionHandler(NotImplementedException.class)
  public ProblemDetail handleNotImplemented(NotImplementedException exception) {
    return problem(HttpStatus.NOT_IMPLEMENTED, exception.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException exception) {
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, "The request body is invalid.");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ProblemDetail handleInvalidParameter(MethodArgumentTypeMismatchException exception) {
    return problem(HttpStatus.BAD_REQUEST, "A request parameter has an invalid value.");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ProblemDetail handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
    return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The request media type is not supported.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ProblemDetail handleUnsupportedMethod(HttpRequestMethodNotSupportedException exception) {
    return problem(HttpStatus.METHOD_NOT_ALLOWED, "The request method is not supported.");
  }

  private static ProblemDetail problem(HttpStatus status, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(status.getReasonPhrase());
    return problem;
  }
}
