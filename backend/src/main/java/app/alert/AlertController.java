package app.alert;

import static app.common.EndpointStub.notImplemented;

import app.common.OpenApiConfig;
import app.common.ProblemDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Live alerts over server-sent events. When a parcel's risk is high, the daily job publishes an
 * alert; a consumer pushes a {@code risk-alert} event to the user on this stream. There is no
 * alerts list in Demo 1.
 *
 * <p>Published for the API contract but not implemented yet, so the call returns 501 with a Problem
 * Details body.
 */
@RestController
@Tag(name = "Alerts", description = "Live risk alerts over server-sent events.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME_NAME)
public class AlertController {

  private static final String PROBLEM_JSON = ProblemDetailResponse.MEDIA_TYPE;

  @Operation(
      summary = "Open the live alerts stream",
      description =
          "Opens an authenticated server-sent events stream. Each high-risk alert arrives as a"
              + " 'risk-alert' event whose data is an AlertEvent. Use a fetch-based SSE client,"
              + " because browser EventSource cannot send the Authorization header.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The stream is open. Events are named 'risk-alert'.",
        content =
            @Content(
                mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                schema = @Schema(implementation = AlertEvent.class))),
    @ApiResponse(
        responseCode = "401",
        description = "The token is missing, invalid or expired.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class))),
    @ApiResponse(
        responseCode = "501",
        description = "Not implemented yet.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class)))
  })
  @GetMapping(path = "/alerts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamAlerts() {
    throw notImplemented();
  }
}
