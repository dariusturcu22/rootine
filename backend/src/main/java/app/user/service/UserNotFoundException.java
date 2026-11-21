package app.user.service;

/**
 * The authenticated token points at a user that no longer exists. Reported as 401 Unauthorized so
 * the caller re-authenticates.
 */
public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException() {
    super("The authenticated user no longer exists.");
  }
}
