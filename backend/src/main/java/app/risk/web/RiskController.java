package app.risk.web;

import static app.common.error.EndpointStub.notImplemented;

import app.common.config.OpenApiConfig;
import app.common.error.ProblemDetailResponse;
import app.risk.dto.HeatmapPoint;
import app.risk.dto.RiskResponse;
import app.risk.service.RiskService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Risk and heatmap reads. The parcel risk endpoint obtains a live result from the AI service. Every
 * endpoint needs a token.
 */
@RestController
@Tag(name = "Risk", description = "Read live parcel risk results and the heatmap.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME_NAME)
public class RiskController {

  private static final String PROBLEM_JSON = ProblemDetailResponse.MEDIA_TYPE;
  private final RiskService riskService;

  public RiskController(RiskService riskService) {
    this.riskService = riskService;
  }

  @Operation(
      summary = "Get a parcel's current risk result",
      description =
          "Loads the owned parcel and calls the AI service for a live weather-based risk.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The current risk result returned by the AI service.",
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
        description = "The parcel does not exist or belongs to another user.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class))),
    @ApiResponse(
        responseCode = "502",
        description = "The AI service is unavailable or returned an error.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class)))
  })
  @GetMapping(path = "/parcels/{id}/risk", produces = MediaType.APPLICATION_JSON_VALUE)
  public RiskResponse getParcelRisk(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return riskService.getParcelRisk(UUID.fromString(jwt.getSubject()), id);
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
