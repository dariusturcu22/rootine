package app.parcel;

import static org.assertj.core.api.Assertions.assertThat;

import app.AbstractIntegrationTest;
import app.parcel.domain.Parcel;
import app.parcel.repository.ParcelRepository;
import app.user.domain.UserAccount;
import app.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Checks that the V2 and V3 migrations apply on a clean PostGIS container and that a parcel polygon
 * can be saved and read back unchanged. Extends {@link AbstractIntegrationTest}, so the PostGIS and
 * RabbitMQ containers start once and Flyway runs every migration before the test.
 */
class ParcelPersistenceIT extends AbstractIntegrationTest {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), SpatialReference.WGS84_SRID);

  @Autowired private UserRepository userRepository;
  @Autowired private ParcelRepository parcelRepository;

  @Test
  void savesAndReadsBackAParcelPolygon() {
    UserAccount owner = savedOwner();
    Polygon polygon = squarePolygon();

    UUID parcelId = UUID.randomUUID();
    Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    parcelRepository.save(new Parcel(parcelId, owner, "North field", "potato", polygon, createdAt));

    Parcel reloaded = parcelRepository.findById(parcelId).orElseThrow();

    assertThat(reloaded.getName()).isEqualTo("North field");
    assertThat(reloaded.getCrop()).isEqualTo("potato");
    assertThat(reloaded.getOwner().getId()).isEqualTo(owner.getId());
    assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);

    Polygon readGeometry = reloaded.getGeometry();
    assertThat(readGeometry.getSRID()).isEqualTo(SpatialReference.WGS84_SRID);
    assertThat(readGeometry.equalsTopo(polygon)).isTrue();
    assertThat(readGeometry.getExteriorRing().getNumPoints())
        .isEqualTo(polygon.getExteriorRing().getNumPoints());
    assertThat(readGeometry.getCoordinates()[0].getX())
        .isEqualTo(polygon.getCoordinates()[0].getX());
    assertThat(readGeometry.getCoordinates()[0].getY())
        .isEqualTo(polygon.getCoordinates()[0].getY());
  }

  @Test
  void listsOnlyTheOwnersParcelsNewestFirst() {
    UserAccount owner = savedOwner();
    UserAccount otherOwner = savedOwner();

    Instant earlier = Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.MILLIS);
    Instant later = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    Parcel older =
        parcelRepository.save(
            new Parcel(UUID.randomUUID(), owner, "Older", "potato", squarePolygon(), earlier));
    Parcel newer =
        parcelRepository.save(
            new Parcel(UUID.randomUUID(), owner, "Newer", "tomato", squarePolygon(), later));
    parcelRepository.save(
        new Parcel(UUID.randomUUID(), otherOwner, "Theirs", "potato", squarePolygon(), later));

    List<Parcel> owned = parcelRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId());

    assertThat(owned).extracting(Parcel::getId).containsExactly(newer.getId(), older.getId());
  }

  private UserAccount savedOwner() {
    return userRepository.save(
        new UserAccount(
            UUID.randomUUID(),
            "owner-" + UUID.randomUUID() + "@example.com",
            "hashed-password",
            Instant.now().truncatedTo(ChronoUnit.MILLIS)));
  }

  /** A small closed square ring in WGS 84, matching the contract's example coordinates. */
  private static Polygon squarePolygon() {
    Coordinate[] ring = {
      new Coordinate(23.59, 46.77),
      new Coordinate(23.60, 46.77),
      new Coordinate(23.60, 46.78),
      new Coordinate(23.59, 46.78),
      new Coordinate(23.59, 46.77)
    };
    Polygon polygon = GEOMETRY_FACTORY.createPolygon(ring);
    polygon.setSRID(SpatialReference.WGS84_SRID);
    return polygon;
  }
}
