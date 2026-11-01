package app.diagnosis.dto;

import app.common.SupportLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The result of one photo sent to the AI service for one parcel.
 *
 * @param id the diagnosis's UUID, set by the backend
 * @param parcelId the parcel the photo belongs to
 * @param status the outcome of the diagnosis
 * @param crop the crop used
 * @param cropCheck whether the photo's crop matched the parcel's crop, or {@code null} when no
 *     check was possible
 * @param topPredictions three predictions; empty when {@code status} is {@code invalid_image} or
 *     {@code crop_not_supported}
 * @param supportLevel how well the crop is supported
 * @param modelVersion the model version, for example {@code plant-v1}
 * @param createdAt when the diagnosis was created, ISO 8601 in UTC, set by the backend
 */
@Schema(name = "DiagnosisResponse", description = "The result of diagnosing one leaf photo.")
public record DiagnosisResponse(
    @Schema(example = "9a1c7f52-6b3e-4c1a-9d0e-1f2a3b4c5d6e") UUID id,
    @Schema(example = "3b241101-e2bb-4255-8caf-4136c566a962") UUID parcelId,
    DiagnosisStatus status,
    @Schema(example = "potato") String crop,
    @Schema(nullable = true) CropCheck cropCheck,
    List<Prediction> topPredictions,
    SupportLevel supportLevel,
    @Schema(example = "plant-v1") String modelVersion,
    @Schema(example = "2026-11-01T08:05:00Z") Instant createdAt) {}
