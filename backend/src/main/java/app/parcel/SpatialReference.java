package app.parcel;

/** Spatial reference constants shared by the parcel geometry code. */
public final class SpatialReference {

  /**
   * SRID for WGS 84 (longitude/latitude in degrees). Parcel polygons are stored and read with this
   * SRID, matching the {@code GEOMETRY(POLYGON, 4326)} column and the GeoJSON the frontend sends.
   */
  public static final int WGS84_SRID = 4326;

  private SpatialReference() {}
}
