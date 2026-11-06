package app.parcel.service;

/** The parcel request contains invalid GeoJSON geometry. */
public class InvalidParcelException extends RuntimeException {

  public InvalidParcelException(String message) {
    super(message);
  }

  public InvalidParcelException(String message, Throwable cause) {
    super(message, cause);
  }
}
