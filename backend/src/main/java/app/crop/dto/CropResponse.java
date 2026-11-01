package app.crop.dto;

import app.common.SupportLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One enabled crop, as returned by {@code GET /crops}. The backend forwards the list from the AI
 * service so the frontend never hardcodes it.
 *
 * @param crop the crop name, lowercase
 * @param diagnosisSupported whether photo diagnosis works for this crop
 * @param weatherSupported whether at least one weather rule exists for this crop
 * @param supportLevel how well the crop is supported
 * @param weatherRules names of the weather rules used, for example {@code hutton}
 * @param classes disease names the model can output for this crop ({@code healthy} is a class)
 */
@Schema(name = "CropResponse", description = "An enabled crop and what it supports.")
public record CropResponse(
    @Schema(example = "potato") String crop,
    @Schema(example = "true") boolean diagnosisSupported,
    @Schema(example = "true") boolean weatherSupported,
    SupportLevel supportLevel,
    @Schema(example = "[\"hutton\"]") List<String> weatherRules,
    @Schema(example = "[\"early_blight\", \"healthy\", \"late_blight\"]") List<String> classes) {}
