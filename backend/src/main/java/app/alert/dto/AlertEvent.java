package app.alert.dto;

import app.risk.dto.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * The {@code data} payload of a {@code risk-alert} server-sent event. Sent when a parcel's risk is
 * high.
 *
 * @param parcelId the affected parcel's UUID
 * @param parcelName the parcel name
 * @param crop the parcel's crop
 * @param level the risk level; always {@code high} for an alert
 * @param riskIndex the risk score, from 0 to 100
 * @param reason a plain-text explanation from the risk result
 * @param generatedAt when the risk result was generated, ISO 8601 in UTC
 */
@Schema(name = "AlertEvent", description = "The data of a 'risk-alert' server-sent event.")
public record AlertEvent(
    @Schema(example = "3b241101-e2bb-4255-8caf-4136c566a962") UUID parcelId,
    @Schema(example = "North field") String parcelName,
    @Schema(example = "potato") String crop,
    RiskLevel level,
    @Schema(example = "78") int riskIndex,
    @Schema(example = "High risk conditions were detected.") String reason,
    @Schema(example = "2026-11-01T06:00:00Z") Instant generatedAt) {}
