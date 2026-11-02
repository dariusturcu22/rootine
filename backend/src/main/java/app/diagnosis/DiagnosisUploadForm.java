package app.diagnosis;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Documentation-only shape of the {@code multipart/form-data} body of {@code POST /diagnoses}. The
 * controller reads the parts directly; this record exists so the OpenAPI document describes the
 * {@code image} and {@code parcelId} parts for the generated frontend types.
 *
 * @param image the leaf photo; JPEG or PNG, up to 10 MB
 * @param parcelId the parcel the photo belongs to; its crop is sent to the AI service
 */
@Schema(name = "DiagnosisUpload", description = "The multipart body used to create a diagnosis.")
public record DiagnosisUploadForm(
    @Schema(type = "string", format = "binary", description = "The leaf photo (JPEG or PNG).")
        byte[] image,
    @Schema(example = "3b241101-e2bb-4255-8caf-4136c566a962") UUID parcelId) {}
