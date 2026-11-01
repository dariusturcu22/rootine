package app.user;

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

/**
 * The current user. Needs a valid token.
 *
 * <p>Published for the API contract but not implemented yet, so the call returns 501 with a Problem
 * Details body.
 */
@RestController
@Tag(name = "Auth", description = "Registration and login.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME_NAME)
public class MeController {

  private static final String PROBLEM_JSON = ProblemDetailResponse.MEDIA_TYPE;

  @Operation(
      summary = "Get the current user",
      description = "Returns the user the token belongs to.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The current user.",
        content = @Content(schema = @Schema(implementation = UserResponse.class))),
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
  @GetMapping(path = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
  public UserResponse getCurrentUser() {
    throw notImplemented();
  }
}
