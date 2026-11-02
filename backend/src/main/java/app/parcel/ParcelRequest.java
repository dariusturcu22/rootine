package app.parcel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /parcels} and {@code PUT /parcels/{id}}. {@code PUT} replaces all three
 * fields, so none can be left out.
 *
 * @param name non-blank parcel name, up to 100 characters
 * @param crop a lowercase crop name, for example {@code potato}; the list comes from {@code GET
 *     /crops}
 * @param geometry a GeoJSON {@code Polygon} in WGS 84
 */
@Schema(name = "ParcelRequest", description = "The fields needed to create or replace a parcel.")
public record ParcelRequest(
    @Schema(example = "North field") @NotBlank @Size(max = ParcelRequest.MAXIMUM_NAME_LENGTH)
        String name,
    @Schema(example = "potato") @NotBlank String crop,
    @Valid @NotNull PolygonGeometry geometry) {

  /** Maximum number of characters a parcel name may have, as stated in the contract. */
  public static final int MAXIMUM_NAME_LENGTH = 100;
}
