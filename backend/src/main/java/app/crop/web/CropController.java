package app.crop.web;

import app.aiclient.AiServiceClient;
import app.aiclient.AiServiceException;
import app.common.SupportLevel;
import app.common.config.OpenApiConfig;
import app.common.error.ProblemDetailResponse;
import app.crop.dto.CropResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The crop list. The backend returns the enabled crops reported by the AI service so the frontend
 * never hardcodes them.
 *
 * <p>The crop list is fetched from the AI service and translated to the backend DTO.
 */
@RestController
@Tag(name = "Crops", description = "The enabled crop list.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME_NAME)
public class CropController {

  private static final String PROBLEM_JSON = ProblemDetailResponse.MEDIA_TYPE;
  private final AiServiceClient aiServiceClient;

  public CropController(AiServiceClient aiServiceClient) {
    this.aiServiceClient = aiServiceClient;
  }

  @Operation(
      summary = "List the enabled crops",
      description = "Returns the enabled crops from the AI service. In Demo 1: tomato and potato.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The enabled crops.",
        content =
            @Content(array = @ArraySchema(schema = @Schema(implementation = CropResponse.class)))),
    @ApiResponse(
        responseCode = "401",
        description = "The token is missing, invalid or expired.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class))),
    @ApiResponse(
        responseCode = "502",
        description = "The AI service cannot provide the list.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class))),
  })
  @GetMapping(path = "/crops", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<CropResponse> listCrops() {
    return aiServiceClient.getCrops().stream().map(CropController::toResponse).toList();
  }

  private static CropResponse toResponse(app.aiclient.CropResponse crop) {
    SupportLevel supportLevel =
        java.util.Arrays.stream(SupportLevel.values())
            .filter(value -> value.jsonValue().equals(crop.supportLevel()))
            .findFirst()
            .orElseThrow(
                () -> new AiServiceException("AI service returned an unknown support level"));
    return new CropResponse(
        crop.crop(),
        crop.diagnosisSupported(),
        crop.weatherSupported(),
        supportLevel,
        crop.weatherRules(),
        crop.classes());
  }
}
