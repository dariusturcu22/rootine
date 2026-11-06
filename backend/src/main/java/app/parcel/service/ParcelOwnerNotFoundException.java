package app.parcel.service;

/** The authenticated user does not have a persisted account. */
public class ParcelOwnerNotFoundException extends RuntimeException {

  public ParcelOwnerNotFoundException() {
    super("The user account was not found.");
  }
}
