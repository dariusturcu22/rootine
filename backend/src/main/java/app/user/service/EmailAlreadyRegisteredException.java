package app.user.service;

/** An account already exists with the requested email address. Reported as 409 Conflict. */
public class EmailAlreadyRegisteredException extends RuntimeException {

  public EmailAlreadyRegisteredException() {
    super("An account with this email already exists.");
  }
}
