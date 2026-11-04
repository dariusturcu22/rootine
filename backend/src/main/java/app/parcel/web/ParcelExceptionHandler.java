package app.parcel.web;

import app.parcel.service.InvalidParcelException;
import app.parcel.service.ParcelOwnerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converts parcel errors into the Problem Details responses promised by the API contract. */
@RestControllerAdvice
public class ParcelExceptionHandler {

  @ExceptionHandler(InvalidParcelException.class)
  public ProblemDetail handleInvalidParcel(InvalidParcelException exception) {
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
  }

  @ExceptionHandler(ParcelOwnerNotFoundException.class)
  public ProblemDetail handleMissingOwner(ParcelOwnerNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, "The parcel request is invalid.");
  }

  private static ProblemDetail problem(HttpStatus status, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(status.getReasonPhrase());
    return problem;
  }
}
