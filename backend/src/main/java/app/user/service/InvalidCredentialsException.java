package app.user.service;

/**
 * Login failed. The same message is used for an unknown email and a wrong password, so a caller
 * cannot learn which emails have an account. Reported as 401 Unauthorized.
 */
public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException() {
    super("Invalid email or password.");
  }
}
