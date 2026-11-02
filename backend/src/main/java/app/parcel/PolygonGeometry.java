package app.parcel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * A GeoJSON {@code Polygon} in WGS 84, as used by parcels. Each position is {@code [longitude,
 * latitude]}. Only {@code Polygon} is accepted, not {@code MultiPolygon}.
 *
 * <p>The outer list is the set of linear rings (the first is the outer ring); each ring is a list
 * of positions; each position is a two-number list {@code [longitude, latitude]}. The ring must be
 * closed (first and last positions equal) and have at least four positions.
 *
 * @param type the GeoJSON geometry type; always {@code Polygon}
 * @param coordinates the polygon rings, each a list of {@code [longitude, latitude]} positions
 */
@Schema(
    name = "PolygonGeometry",
    description = "A GeoJSON Polygon in WGS 84 (longitude, latitude).")
public record PolygonGeometry(
    @Schema(allowableValues = "Polygon", example = "Polygon") @NotNull String type,
    @Schema(
            example =
                "[[[23.59, 46.77], [23.60, 46.77], [23.60, 46.78], [23.59, 46.78], [23.59, 46.77]]]")
        @NotEmpty
        List<List<List<Double>>> coordinates) {}
