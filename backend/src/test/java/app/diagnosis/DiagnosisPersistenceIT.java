package app.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;

import app.AbstractIntegrationTest;
import app.common.SupportLevel;
import app.diagnosis.domain.Diagnosis;
import app.diagnosis.domain.DiagnosisResult;
import app.diagnosis.dto.CropCheck;
import app.diagnosis.dto.DiagnosisResponse;
import app.diagnosis.dto.DiagnosisStatus;
import app.diagnosis.dto.Prediction;
import app.diagnosis.mapper.DiagnosisMapper;
import app.diagnosis.repository.DiagnosisRepository;
import app.parcel.SpatialReference;
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
 * Checks that the V4 migration applies on a clean PostGIS container and that a diagnosis, including
 * its JSON result document, can be saved and read back. Also checks the mapper flattens the stored
 * result into the response DTO, so the controller never has to touch the entity.
 */
class DiagnosisPersistenceIT extends AbstractIntegrationTest {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), SpatialReference.WGS84_SRID);

  @Autowired private UserRepository userRepository;
  @Autowired private ParcelRepository parcelRepository;
  @Autowired private DiagnosisRepository diagnosisRepository;
  @Autowired private DiagnosisMapper diagnosisMapper;

  @Test
  void savesAndReadsBackADiagnosisWithItsJsonResult() {
    Parcel parcel = savedParcel();
    DiagnosisResult result =
        new DiagnosisResult(
            DiagnosisStatus.CONFIDENT,
            "potato",
            CropCheck.OK,
            List.of(
                new Prediction("potato_early_blight", "early_blight", 0.92),
                new Prediction("potato_late_blight", "late_blight", 0.05),
                new Prediction("potato_healthy", "healthy", 0.03)),
            SupportLevel.LAB_ONLY);

    UUID diagnosisId = UUID.randomUUID();
    Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    diagnosisRepository.save(
        new Diagnosis(diagnosisId, parcel, "uploads/leaf-1.jpg", result, "plant-v1", createdAt));

    Diagnosis reloaded = diagnosisRepository.findById(diagnosisId).orElseThrow();

    assertThat(reloaded.getPhotoPath()).isEqualTo("uploads/leaf-1.jpg");
    assertThat(reloaded.getModelVersion()).isEqualTo("plant-v1");
    assertThat(reloaded.getParcel().getId()).isEqualTo(parcel.getId());
    assertThat(reloaded.getResult().status()).isEqualTo(DiagnosisStatus.CONFIDENT);
    assertThat(reloaded.getResult().cropCheck()).isEqualTo(CropCheck.OK);
    assertThat(reloaded.getResult().supportLevel()).isEqualTo(SupportLevel.LAB_ONLY);
    assertThat(reloaded.getResult().topPredictions()).hasSize(3);
    assertThat(reloaded.getResult().topPredictions().get(0).label())
        .isEqualTo("potato_early_blight");

    DiagnosisResponse response = diagnosisMapper.toResponse(reloaded);
    assertThat(response.id()).isEqualTo(diagnosisId);
    assertThat(response.parcelId()).isEqualTo(parcel.getId());
    assertThat(response.status()).isEqualTo(DiagnosisStatus.CONFIDENT);
    assertThat(response.crop()).isEqualTo("potato");
    assertThat(response.modelVersion()).isEqualTo("plant-v1");
    assertThat(response.topPredictions()).hasSize(3);
  }

  private Parcel savedParcel() {
    UserAccount owner =
        userRepository.save(
            new UserAccount(
                UUID.randomUUID(),
                "owner-" + UUID.randomUUID() + "@example.com",
                "hashed-password",
                Instant.now().truncatedTo(ChronoUnit.MILLIS)));
    return parcelRepository.save(
        new Parcel(
            UUID.randomUUID(),
            owner,
            "North field",
            "potato",
            squarePolygon(),
            Instant.now().truncatedTo(ChronoUnit.MILLIS)));
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
