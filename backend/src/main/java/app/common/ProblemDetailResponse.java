package app.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Documentation-only view of the RFC 9457 Problem Details error body. The runtime uses Spring's
 * {@link org.springframework.http.ProblemDetail}; this record exists so the OpenAPI document (and
 * therefore the generated frontend types) describes the error shape with the same field names.
 *
 * @param type a URI that identifies the problem type, or {@code about:blank} when there is none
 * @param title a short, human-readable summary of the problem type
 * @param status the HTTP status code
 * @param detail a human-readable explanation specific to this occurrence of the problem
 */
@Schema(name = "ProblemDetail", description = "RFC 9457 error body (application/problem+json).")
public record ProblemDetailResponse(
    @Schema(example = "about:blank") String type,
    @Schema(example = "Not Implemented") String title,
    @Schema(example = "501") int status,
    @Schema(example = "This endpoint is published in the API contract but is not implemented yet.")
        String detail) {

  /** Media type of a Problem Details body, as defined by RFC 9457. */
  public static final String MEDIA_TYPE = "application/problem+json";
}
