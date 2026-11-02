package app.common;

/**
 * Helper for controller methods that are published in the API contract but not implemented yet.
 *
 * <p>Every stub endpoint throws {@link NotImplementedException} through {@link #notImplemented()},
 * which the global handler turns into a 501 Problem Details response. This keeps the published
 * endpoints honest: they appear in the OpenAPI document so the frontend can generate types, but
 * they never pretend to return real data.
 */
public final class EndpointStub {

  /** Detail message used in the 501 Problem Details body for every unimplemented endpoint. */
  public static final String NOT_IMPLEMENTED_DETAIL =
      "This endpoint is published in the API contract but is not implemented yet.";

  private EndpointStub() {}

  /**
   * Signals that the calling endpoint is not implemented yet.
   *
   * @return never returns; the return type lets callers write {@code throw notImplemented();} so
   *     the compiler knows the method ends there
   */
  public static NotImplementedException notImplemented() {
    return new NotImplementedException(NOT_IMPLEMENTED_DETAIL);
  }
}
