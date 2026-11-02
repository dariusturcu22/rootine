package app.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The latest stored risk result for a parcel. For a crop without a weather model, {@code level} is
 * {@code not_modelled}, {@code riskIndex} is 0 and {@code diseases} is empty.
 *
 * @param parcelId the parcel the result belongs to
 * @param crop the parcel's crop
 * @param level the overall risk level
 * @param riskIndex the overall risk score, from 0 to 100
 * @param reason a plain-text explanation from the risk result
 * @param diseases the per-disease risks
 * @param generatedAt when the result was generated, ISO 8601 in UTC
 */
@Schema(name = "RiskResponse", description = "The latest stored risk result for a parcel.")
public record RiskResponse(
    @Schema(example = "3b241101-e2bb-4255-8caf-4136c566a962") UUID parcelId,
    @Schema(example = "potato") String crop,
    RiskLevel level,
    @Schema(example = "42") int riskIndex,
    @Schema(
            example =
                "Mock response: fixed medium risk, real weather rules are not implemented yet.")
        String reason,
    List<DiseaseRisk> diseases,
    @Schema(example = "2026-11-01T06:00:00Z") Instant generatedAt) {}
