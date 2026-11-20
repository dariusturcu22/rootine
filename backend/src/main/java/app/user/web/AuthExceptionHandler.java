package app.user.web;

import app.user.service.EmailAlreadyRegisteredException;
import app.user.service.InvalidCredentialsException;
import app.user.service.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts auth errors into the Problem Details responses promised by the API contract. Scoped to
 * the auth controllers so it does not collide with other feature handlers that also map validation
 * errors.
 */
@RestControllerAdvice(assignableTypes = {AuthController.class, MeController.class})
public class AuthExceptionHandler {

  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  public ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
    return problem(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception) {
    return problem(HttpStatus.UNAUTHORIZED, exception.getMessage());
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ProblemDetail handleUserNotFound(UserNotFoundException exception) {
    return problem(HttpStatus.UNAUTHORIZED, exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, "The request is invalid.");
  }

  private static ProblemDetail problem(HttpStatus status, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(status.getReasonPhrase());
    return problem;
  }
}
