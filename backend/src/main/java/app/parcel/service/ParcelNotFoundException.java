package app.parcel.service;

/** The parcel does not exist or does not belong to the authenticated user. */
public class ParcelNotFoundException extends RuntimeException {

  public ParcelNotFoundException() {
    super("The parcel was not found.");
  }
}
