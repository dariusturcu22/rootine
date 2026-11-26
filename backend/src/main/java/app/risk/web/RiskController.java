package app.risk.web;

import static app.common.error.EndpointStub.notImplemented;

import app.common.config.OpenApiConfig;
import app.common.error.ProblemDetailResponse;
import app.risk.dto.HeatmapPoint;
import app.risk.dto.RiskResponse;
import app.risk.service.RiskService;
import io.swagger.v3.oas.annotations.Operation;
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
 * The per-parcel endpoint requests a fresh risk result from the AI service. Every endpoint needs a
 * token.
 */
@RestController
@Tag(name = "Risk", description = "Request current per-parcel weather risk.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME_NAME)
public class RiskController {

  private static final String PROBLEM_JSON = ProblemDetailResponse.MEDIA_TYPE;
  private final RiskService riskService;

  public RiskController(RiskService riskService) {
    this.riskService = riskService;
  }

  @Operation(
      summary = "Get a parcel's current risk result",
      description = "Requests the current weather risk result from the AI service.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The current weather risk result.",
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

  @Operation(summary = "Get the risk heatmap", description = "Not available in Demo 1.")
  @ApiResponses({
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
