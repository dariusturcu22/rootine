package app.parcel.mapper;

import app.parcel.SpatialReference;
import app.parcel.domain.Parcel;
import app.parcel.dto.ParcelResponse;
import app.parcel.dto.PolygonGeometry;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.Mapper;

/**
 * Maps between the {@link Parcel} entity and the parcel DTOs, so controllers never expose the
 * entity. The hard part is the geometry: the entity holds a JTS {@link Polygon}, while the DTOs use
 * a GeoJSON {@link PolygonGeometry} whose positions are {@code [longitude, latitude]}.
 */
@Mapper(componentModel = "spring")
public interface ParcelMapper {

  String POLYGON_TYPE = "Polygon";

  /** One shared factory fixes the SRID to WGS 84 for every polygon this mapper builds. */
  GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), SpatialReference.WGS84_SRID);

  /**
   * Maps the entity to the response DTO. MapStruct uses {@link #toPolygonGeometry} for geometry.
   */
  ParcelResponse toResponse(Parcel parcel);

  /** Converts a JTS polygon to a GeoJSON polygon with {@code [longitude, latitude]} positions. */
  default PolygonGeometry toPolygonGeometry(Polygon polygon) {
    if (polygon == null) {
      return null;
    }
    List<List<List<Double>>> rings = new ArrayList<>();
    rings.add(toPositions(polygon.getExteriorRing().getCoordinates()));
    for (int holeIndex = 0; holeIndex < polygon.getNumInteriorRing(); holeIndex++) {
      rings.add(toPositions(polygon.getInteriorRingN(holeIndex).getCoordinates()));
    }
    return new PolygonGeometry(POLYGON_TYPE, rings);
  }

  /** Converts a GeoJSON polygon back to a JTS polygon tagged with the WGS 84 SRID. */
  default Polygon toPolygon(PolygonGeometry polygonGeometry) {
    if (polygonGeometry == null || polygonGeometry.coordinates() == null) {
      return null;
    }
    List<List<List<Double>>> rings = polygonGeometry.coordinates();
    LinearRing shell = toLinearRing(rings.get(0));
    LinearRing[] holes = new LinearRing[rings.size() - 1];
    for (int holeIndex = 1; holeIndex < rings.size(); holeIndex++) {
      holes[holeIndex - 1] = toLinearRing(rings.get(holeIndex));
    }
    return GEOMETRY_FACTORY.createPolygon(shell, holes);
  }

  private List<List<Double>> toPositions(Coordinate[] coordinates) {
    List<List<Double>> positions = new ArrayList<>(coordinates.length);
    for (Coordinate coordinate : coordinates) {
      positions.add(List.of(coordinate.getX(), coordinate.getY()));
    }
    return positions;
  }

  private LinearRing toLinearRing(List<List<Double>> positions) {
    Coordinate[] coordinates = new Coordinate[positions.size()];
    for (int index = 0; index < positions.size(); index++) {
      List<Double> position = positions.get(index);
      double longitude = position.get(0);
      double latitude = position.get(1);
      coordinates[index] = new Coordinate(longitude, latitude);
    }
    return GEOMETRY_FACTORY.createLinearRing(coordinates);
  }
}
