package app.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The risk for one disease, from one weather rule model.
 *
 * @param disease the disease name, for example {@code late_blight}
 * @param model the weather rule model that produced this risk, for example {@code hutton}
 * @param level the risk level for this disease
 * @param riskIndex the risk score for this disease, from 0 to 100
 */
@Schema(name = "DiseaseRisk", description = "The risk for one disease from one weather model.")
public record DiseaseRisk(
    @Schema(example = "late_blight") String disease,
    @Schema(example = "hutton") String model,
    RiskLevel level,
    @Schema(example = "42") int riskIndex) {}
