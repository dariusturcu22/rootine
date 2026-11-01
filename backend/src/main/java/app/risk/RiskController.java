package app.risk;

import static app.common.EndpointStub.notImplemented;

import app.common.OpenApiConfig;
import app.common.ProblemDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Risk and heatmap reads. The backend stores the latest risk result the AI service returns for each
 * parcel; a daily job refreshes it. Every endpoint needs a token.
 *
 * <p>Published for the API contract but not implemented yet, so every call returns 501 with a
 * Problem Details body.
 */
@RestController
@Tag(name = "Risk", description = "Read stored risk results and the heatmap.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME_NAME)
public class RiskController {

  private static final String PROBLEM_JSON = ProblemDetailResponse.MEDIA_TYPE;

  @Operation(
      summary = "Get a parcel's latest risk result",
      description = "Returns the latest stored risk result for the parcel.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The latest stored risk result.",
        content = @Content(schema = @Schema(implementation = RiskResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "The token is missing, invalid or expired.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description =
            "The parcel does not exist, belongs to another user, or has no stored risk result yet.",
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
  @GetMapping(path = "/parcels/{id}/risk", produces = MediaType.APPLICATION_JSON_VALUE)
  public RiskResponse getParcelRisk(@PathVariable UUID id) {
    throw notImplemented();
  }

  @Operation(
      summary = "Get the risk heatmap",
      description =
          "Returns one point per parcel that has a stored risk result, at the parcel centre."
              + " Parcels not yet processed by the daily job are omitted.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "One point per parcel with a stored risk result; an empty list if none.",
        content =
            @Content(array = @ArraySchema(schema = @Schema(implementation = HeatmapPoint.class)))),
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
  @GetMapping(path = "/risk/heatmap", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<HeatmapPoint> getHeatmap() {
    throw notImplemented();
  }
}
