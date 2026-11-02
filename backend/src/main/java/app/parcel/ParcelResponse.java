package app.parcel;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * A parcel as returned by the parcel endpoints.
 *
 * @param id the parcel's UUID, set by the backend
 * @param name the parcel name
 * @param crop the lowercase crop name
 * @param geometry the GeoJSON {@code Polygon} in WGS 84
 * @param createdAt when the parcel was created, ISO 8601 in UTC, set by the backend
 */
@Schema(name = "ParcelResponse", description = "A parcel with its crop and polygon.")
public record ParcelResponse(
    @Schema(example = "3b241101-e2bb-4255-8caf-4136c566a962") UUID id,
    @Schema(example = "North field") String name,
    @Schema(example = "potato") String crop,
    PolygonGeometry geometry,
    @Schema(example = "2026-11-01T08:00:00Z") Instant createdAt) {}
