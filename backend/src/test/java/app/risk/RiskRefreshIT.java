package app.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.AbstractIntegrationTest;
import app.aiclient.AiServiceClient;
import app.aiclient.AiServiceException;
import app.aiclient.DiseaseRisk;
import app.aiclient.RiskRequest;
import app.parcel.SpatialReference;
import app.parcel.domain.Parcel;
import app.parcel.repository.ParcelRepository;
import app.risk.domain.ParcelRisk;
import app.risk.repository.ParcelRiskRepository;
import app.risk.service.RiskRefreshService;
import app.user.domain.UserAccount;
import app.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Applies the V5 migration on a clean PostGIS container and checks the risk refresh: results are
 * stored for a processed parcel, the stored JSON disease list reads back, and a per-parcel AI
 * failure does not overwrite that parcel's last good result.
 *
 * <p>Integration tests share one database, so other tests may have created parcels. These tests
 * therefore assert on their own parcels by id and never on the global parcel count, and the AI
 * client is stubbed by the crop in the request so unrelated parcels always get a harmless result.
 */
class RiskRefreshIT extends AbstractIntegrationTest {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), SpatialReference.WGS84_SRID);

  @Autowired private UserRepository userRepository;
  @Autowired private ParcelRepository parcelRepository;
  @Autowired private ParcelRiskRepository parcelRiskRepository;
  @Autowired private RiskRefreshService riskRefreshService;
  @MockitoBean private AiServiceClient aiServiceClient;

  private final List<UUID> createdParcelIds = new ArrayList<>();
  private final List<UUID> createdOwnerIds = new ArrayList<>();

  /** AI results keyed by crop. Crops not in the map get a harmless default; failing crops throw. */
  private final Map<String, app.aiclient.RiskResponse> riskByCrop = new ConcurrentHashMap<>();

  private final Set<String> failingCrops = ConcurrentHashMap.newKeySet();

  @BeforeEach
  void stubAiByCrop() {
    when(aiServiceClient.getRisk(any(RiskRequest.class)))
        .thenAnswer(
            invocation -> {
              RiskRequest request = invocation.getArgument(0);
              String crop = request.crop();
              if (failingCrops.contains(crop)) {
                throw new AiServiceException("AI down for " + crop);
              }
              // Unrelated parcels (from other tests) get a harmless low-risk default.
              return riskByCrop.getOrDefault(crop, aiRisk(crop, "low", 10));
            });
  }

  @AfterEach
  void removeCreatedData() {
    createdParcelIds.forEach(parcelRiskRepository::deleteById);
    createdParcelIds.forEach(parcelRepository::deleteById);
    createdOwnerIds.forEach(userRepository::deleteById);
  }

  @Test
  void storesTheLatestRiskForTheProcessedParcel() {
    // Unique crops so the AI stub answers per parcel regardless of other parcels in the shared
    // database.
    String cropA = uniqueCrop();
    String cropB = uniqueCrop();
    Parcel first = savedParcel(cropA);
    Parcel second = savedParcel(cropB);
    riskByCrop.put(cropA, aiRisk(cropA, "medium", 42));
    riskByCrop.put(cropB, aiRisk(cropB, "high", 78));

    riskRefreshService.refreshAllParcels();

    ParcelRisk firstRisk = parcelRiskRepository.findById(first.getId()).orElseThrow();
    assertThat(firstRisk.getLevel()).isEqualTo("medium");
    assertThat(firstRisk.getRiskIndex()).isEqualTo(42);
    assertThat(firstRisk.getDiseases()).hasSize(1);
    assertThat(firstRisk.getDiseases().get(0).disease()).isEqualTo("late_blight");
    assertThat(parcelRiskRepository.findById(second.getId()).orElseThrow().getRiskIndex())
        .isEqualTo(78);
  }

  @Test
  void keepsTheLastGoodResultWhenTheAiCallFails() {
    String crop = uniqueCrop();
    Parcel parcel = savedParcel(crop);
    riskByCrop.put(crop, aiRisk(crop, "medium", 42));

    // First refresh: this parcel gets a good result stored.
    riskRefreshService.refreshAllParcels();
    assertThat(parcelRiskRepository.findById(parcel.getId()).orElseThrow().getRiskIndex())
        .isEqualTo(42);

    // Second refresh: the AI call for this parcel's crop fails; its stored result must not change.
    failingCrops.add(crop);
    riskRefreshService.refreshAllParcels();

    ParcelRisk risk = parcelRiskRepository.findById(parcel.getId()).orElseThrow();
    assertThat(risk.getLevel()).isEqualTo("medium");
    assertThat(risk.getRiskIndex()).isEqualTo(42);
  }

  private Parcel savedParcel(String crop) {
    UUID ownerId = UUID.randomUUID();
    UserAccount owner =
        userRepository.save(
            new UserAccount(
                ownerId,
                "owner-" + ownerId + "@example.com",
                "hash",
                Instant.now().truncatedTo(ChronoUnit.MILLIS)));
    createdOwnerIds.add(ownerId);
    Parcel parcel =
        parcelRepository.save(
            new Parcel(
                UUID.randomUUID(),
                owner,
                "Field",
                crop,
                squarePolygon(),
                Instant.now().truncatedTo(ChronoUnit.MILLIS)));
    createdParcelIds.add(parcel.getId());
    return parcel;
  }

  private static String uniqueCrop() {
    return "crop-" + UUID.randomUUID();
  }

  private static app.aiclient.RiskResponse aiRisk(String crop, String level, int riskIndex) {
    return new app.aiclient.RiskResponse(
        crop,
        level,
        riskIndex,
        "Mock " + level + " risk.",
        List.of(new DiseaseRisk("late_blight", "hutton", level, riskIndex)),
        Instant.parse("2026-11-20T06:00:00Z"));
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
