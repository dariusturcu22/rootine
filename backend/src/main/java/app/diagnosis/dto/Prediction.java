package app.diagnosis.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One predicted disease for a diagnosis.
 *
 * @param label the full label, {@code <crop>_<disease>}, for example {@code potato_early_blight}
 * @param disease the disease name, for example {@code early_blight}
 * @param probability the model's probability for this label, from 0 to 1
 */
@Schema(name = "Prediction", description = "One predicted disease and its probability.")
public record Prediction(
    @Schema(example = "potato_early_blight") String label,
    @Schema(example = "early_blight") String disease,
    @Schema(example = "0.92") double probability) {}
