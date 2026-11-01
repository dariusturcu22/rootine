package app.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * One heatmap point for a parcel: its centre and its latest risk result.
 *
 * @param parcelId the parcel the point belongs to
 * @param name the parcel name
 * @param crop the parcel's crop
 * @param latitude the parcel centre latitude, WGS 84
 * @param longitude the parcel centre longitude, WGS 84
 * @param level the overall risk level
 * @param riskIndex the overall risk score, from 0 to 100
 * @param generatedAt when the result was generated, ISO 8601 in UTC
 */
@Schema(name = "HeatmapPoint", description = "One parcel's centre and its latest risk result.")
public record HeatmapPoint(
    @Schema(example = "3b241101-e2bb-4255-8caf-4136c566a962") UUID parcelId,
    @Schema(example = "North field") String name,
    @Schema(example = "potato") String crop,
    @Schema(example = "46.775") double latitude,
    @Schema(example = "23.595") double longitude,
    RiskLevel level,
    @Schema(example = "42") int riskIndex,
    @Schema(example = "2026-11-01T06:00:00Z") Instant generatedAt) {}
