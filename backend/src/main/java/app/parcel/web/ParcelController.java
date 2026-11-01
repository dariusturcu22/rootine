package app.parcel.web;

import static app.common.error.EndpointStub.notImplemented;

import app.common.config.OpenApiConfig;
import app.common.error.ProblemDetailResponse;
import app.parcel.dto.ParcelRequest;
import app.parcel.dto.ParcelResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Parcels: a crop and a polygon per parcel. A user sees only their own parcels, so every endpoint
 * needs a token.
 *
 * <p>Published for the API contract but not implemented yet, so every call returns 501 with a
 * Problem Details body.
 */
@RestController
@RequestMapping(path = "/parcels", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Parcels", description = "Create, read, replace and delete parcels.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME_NAME)
public class ParcelController {

  private static final String PROBLEM_JSON = ProblemDetailResponse.MEDIA_TYPE;

  @Operation(summary = "Create a parcel")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "The parcel was created.",
        content = @Content(schema = @Schema(implementation = ParcelResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "The token is missing, invalid or expired.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description =
            "The body is invalid: a blank or too long name, a missing crop, or an invalid"
                + " polygon.",
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
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public ParcelResponse createParcel(@Valid @RequestBody ParcelRequest parcelRequest) {
    throw notImplemented();
  }

  @Operation(
      summary = "List the current user's parcels",
      description = "Returns the user's parcels, newest first. No pagination in Demo 1.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The user's parcels, newest first.",
        content =
            @Content(
                array = @ArraySchema(schema = @Schema(implementation = ParcelResponse.class)))),
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
  @GetMapping
  public List<ParcelResponse> listParcels() {
    throw notImplemented();
  }

  @Operation(summary = "Get one parcel")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The parcel.",
        content = @Content(schema = @Schema(implementation = ParcelResponse.class))),
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
        responseCode = "501",
        description = "Not implemented yet.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class)))
  })
  @GetMapping("/{id}")
  public ParcelResponse getParcel(@PathVariable UUID id) {
    throw notImplemented();
  }

  @Operation(
      summary = "Replace a parcel",
      description = "Replaces all three fields, so none can be left out.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The replaced parcel.",
        content = @Content(schema = @Schema(implementation = ParcelResponse.class))),
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
        responseCode = "422",
        description =
            "The body is invalid: a blank or too long name, a missing crop, or an invalid"
                + " polygon.",
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
  @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ParcelResponse replaceParcel(
      @PathVariable UUID id, @Valid @RequestBody ParcelRequest parcelRequest) {
    throw notImplemented();
  }

  @Operation(
      summary = "Delete a parcel",
      description = "Deleting a parcel also deletes its diagnoses and its stored risk results.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "The parcel was deleted."),
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
        responseCode = "501",
        description = "Not implemented yet.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class)))
  })
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteParcel(@PathVariable UUID id) {
    throw notImplemented();
  }
}
