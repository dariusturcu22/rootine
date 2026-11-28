package app.risk;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.AbstractIntegrationTest;
import app.parcel.SpatialReference;
import app.parcel.domain.Parcel;
import app.parcel.repository.ParcelRepository;
import app.risk.domain.ParcelRisk;
import app.risk.repository.ParcelRiskRepository;
import app.user.domain.UserAccount;
import app.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Checks that the heatmap reads only the authenticated user's latest stored parcel risks. */
@AutoConfigureMockMvc
class RiskHeatmapIT extends AbstractIntegrationTest {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), SpatialReference.WGS84_SRID);

  @Autowired private UserRepository userRepository;
  @Autowired private ParcelRepository parcelRepository;
  @Autowired private ParcelRiskRepository parcelRiskRepository;
  @Autowired private MockMvc mockMvc;

  private final List<UUID> createdParcelIds = new ArrayList<>();
  private final List<UUID> createdOwnerIds = new ArrayList<>();

  @AfterEach
  void removeCreatedData() {
    createdParcelIds.forEach(parcelRiskRepository::deleteById);
    createdParcelIds.forEach(parcelRepository::deleteById);
    createdOwnerIds.forEach(userRepository::deleteById);
  }

  @Test
  void returnsOwnedParcelsWithStoredRisksAndOmitsParcelsWithoutResults() throws Exception {
    UserAccount owner = savedOwner();
    UserAccount otherOwner = savedOwner();
    Parcel firstParcel = savedParcel(owner, "North field", "potato");
    Parcel secondParcel = savedParcel(owner, "South field", "tomato");
    Parcel withoutRisk = savedParcel(owner, "New field", "potato");
    Parcel otherUsersParcel = savedParcel(otherOwner, "Private field", "tomato");
    savedRisk(firstParcel, "medium", 42, Instant.parse("2026-11-20T06:00:00Z"));
    savedRisk(secondParcel, "high", 78, Instant.parse("2026-11-20T06:00:00Z"));
    savedRisk(otherUsersParcel, "high", 99, Instant.parse("2026-11-20T06:00:00Z"));

    mockMvc
        .perform(
            get("/risk/heatmap").with(jwt().jwt(token -> token.subject(owner.getId().toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(
            jsonPath(
                "$[*].parcelId",
                containsInAnyOrder(
                    firstParcel.getId().toString(), secondParcel.getId().toString())))
        .andExpect(jsonPath("$[*].parcelId", not(hasItem(withoutRisk.getId().toString()))))
        .andExpect(jsonPath("$[*].parcelId", not(hasItem(otherUsersParcel.getId().toString()))))
        .andExpect(jsonPath("$[*].latitude", everyItem(closeTo(46.775, 0.000001))))
        .andExpect(jsonPath("$[*].longitude", everyItem(closeTo(23.595, 0.000001))));
  }

  @Test
  void returnsEmptyListWhenUserHasNoParcels() throws Exception {
    UserAccount owner = savedOwner();

    mockMvc
        .perform(
            get("/risk/heatmap").with(jwt().jwt(token -> token.subject(owner.getId().toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void returnsLatestStoredRiskForParcel() throws Exception {
    UserAccount owner = savedOwner();
    Parcel parcel = savedParcel(owner, "North field", "potato");
    ParcelRisk storedRisk = savedRisk(parcel, "low", 10, Instant.parse("2026-11-19T06:00:00Z"));
    storedRisk.setLevel("high");
    storedRisk.setRiskIndex(87);
    storedRisk.setGeneratedAt(Instant.parse("2026-11-20T06:00:00Z"));
    parcelRiskRepository.save(storedRisk);

    mockMvc
        .perform(
            get("/risk/heatmap").with(jwt().jwt(token -> token.subject(owner.getId().toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].parcelId").value(parcel.getId().toString()))
        .andExpect(jsonPath("$[0].level").value("high"))
        .andExpect(jsonPath("$[0].riskIndex").value(87))
        .andExpect(jsonPath("$[0].generatedAt").value("2026-11-20T06:00:00Z"));
  }

  private UserAccount savedOwner() {
    UUID ownerId = UUID.randomUUID();
    UserAccount owner =
        userRepository.save(
            new UserAccount(
                ownerId,
                "owner-" + ownerId + "@example.com",
                "hash",
                Instant.now().truncatedTo(ChronoUnit.MILLIS)));
    createdOwnerIds.add(ownerId);
    return owner;
  }

  private Parcel savedParcel(UserAccount owner, String name, String crop) {
    Parcel parcel =
        parcelRepository.save(
            new Parcel(
                UUID.randomUUID(),
                owner,
                name,
                crop,
                squarePolygon(),
                Instant.now().truncatedTo(ChronoUnit.MILLIS)));
    createdParcelIds.add(parcel.getId());
    return parcel;
  }

  private ParcelRisk savedRisk(Parcel parcel, String level, int riskIndex, Instant generatedAt) {
    return parcelRiskRepository.save(
        new ParcelRisk(parcel, level, riskIndex, "Stored risk.", List.of(), generatedAt));
  }

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
