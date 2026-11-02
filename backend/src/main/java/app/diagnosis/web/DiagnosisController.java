package app.diagnosis.web;

import static app.common.error.EndpointStub.notImplemented;

import app.common.config.OpenApiConfig;
import app.common.error.ProblemDetailResponse;
import app.diagnosis.dto.DiagnosisResponse;
import app.diagnosis.dto.DiagnosisUploadForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Diagnoses: the result of one photo sent to the AI service for one parcel. A user sees only their
 * own diagnoses, so every endpoint needs a token.
 *
 * <p>Published for the API contract but not implemented yet, so every call returns 501 with a
 * Problem Details body.
 */
@RestController
@RequestMapping(path = "/diagnoses", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Diagnoses", description = "Upload a leaf photo and read diagnosis results.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME_NAME)
public class DiagnosisController {

  private static final String PROBLEM_JSON = ProblemDetailResponse.MEDIA_TYPE;

  @Operation(
      summary = "Create a diagnosis from a leaf photo",
      description =
          "Uploads a JPEG or PNG photo (up to 10 MB) for a parcel. The backend saves the photo and"
              + " calls the AI service with the parcel's crop.")
  @RequestBody(
      content =
          @Content(
              mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
              schema = @Schema(implementation = DiagnosisUploadForm.class)))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "The diagnosis was created.",
        content = @Content(schema = @Schema(implementation = DiagnosisResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "A required part is missing or the file is not a JPEG or PNG image.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class))),
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
        responseCode = "413",
        description = "The file is larger than 10 MB.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class))),
    @ApiResponse(
        responseCode = "502",
        description = "The AI service is unavailable or returned an error. Nothing is saved.",
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
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public DiagnosisResponse createDiagnosis(
      @RequestPart("image") MultipartFile image, @RequestPart("parcelId") UUID parcelId) {
    throw notImplemented();
  }

  @Operation(
      summary = "List diagnoses",
      description = "Returns the user's diagnoses, newest first. No pagination in Demo 1.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The user's diagnoses, newest first.",
        content =
            @Content(
                array = @ArraySchema(schema = @Schema(implementation = DiagnosisResponse.class)))),
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
            "A parcelId is given and the parcel does not exist or belongs to another user.",
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
  @GetMapping
  public List<DiagnosisResponse> listDiagnoses(
      @Parameter(description = "List only the diagnoses of this parcel.")
          @RequestParam(name = "parcelId", required = false)
          UUID parcelId) {
    throw notImplemented();
  }

  @Operation(summary = "Get one diagnosis")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The diagnosis.",
        content = @Content(schema = @Schema(implementation = DiagnosisResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "The token is missing, invalid or expired.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "The diagnosis does not exist or belongs to another user.",
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
  @GetMapping("/{id}")
  public DiagnosisResponse getDiagnosis(@PathVariable UUID id) {
    throw notImplemented();
  }
}
