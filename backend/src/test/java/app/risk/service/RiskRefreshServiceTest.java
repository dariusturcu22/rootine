package app.risk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.aiclient.AiServiceClient;
import app.aiclient.AiServiceException;
import app.aiclient.DiseaseRisk;
import app.aiclient.RiskRequest;
import app.alert.AlertMessage;
import app.alert.AlertPublisher;
import app.parcel.domain.Parcel;
import app.parcel.repository.ParcelRepository;
import app.risk.domain.ParcelRisk;
import app.risk.repository.ParcelRiskRepository;
import app.user.domain.UserAccount;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Unit tests for the scheduled risk refresh: it stores the AI result for each parcel, handles
 * not-modelled crops, and isolates per-parcel AI failures so one failure neither stops the rest nor
 * overwrites that parcel's last good result.
 */
class RiskRefreshServiceTest {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), 4326);

  private final ParcelRepository parcelRepository = mock(ParcelRepository.class);
  private final ParcelRiskRepository parcelRiskRepository = mock(ParcelRiskRepository.class);
  private final AiServiceClient aiServiceClient = mock(AiServiceClient.class);
  private final AlertPublisher alertPublisher = mock(AlertPublisher.class);
  private final PlatformTransactionManager transactionManager =
      mock(PlatformTransactionManager.class);

  private final RiskRefreshService service =
      new RiskRefreshService(
          parcelRepository,
          parcelRiskRepository,
          aiServiceClient,
          alertPublisher,
          transactionManager);

  RiskRefreshServiceTest() {
    when(transactionManager.getTransaction(ArgumentMatchers.any()))
        .thenReturn(new SimpleTransactionStatus());
  }

  @Test
  void storesTheAiResultForEveryParcelAcrossUsers() {
    Parcel potato = parcel("potato");
    Parcel tomato = parcel("tomato");
    when(parcelRepository.findAll()).thenReturn(List.of(potato, tomato));
    when(parcelRepository.getReferenceById(potato.getId())).thenReturn(potato);
    when(parcelRepository.getReferenceById(tomato.getId())).thenReturn(tomato);
    when(parcelRiskRepository.findById(any())).thenReturn(Optional.empty());
    when(aiServiceClient.getRisk(any(RiskRequest.class)))
        .thenReturn(
            new app.aiclient.RiskResponse(
                "potato",
                "medium",
                42,
                "Mock medium risk.",
                List.of(new DiseaseRisk("late_blight", "hutton", "medium", 42)),
                Instant.parse("2026-11-20T06:00:00Z")));

    int stored = service.refreshAllParcels();

    assertThat(stored).isEqualTo(2);
    verify(parcelRiskRepository, org.mockito.Mockito.times(2)).save(any(ParcelRisk.class));
    verify(alertPublisher, never()).publish(any(AlertMessage.class));
  }

  @Test
  void storesNotModelledResultsWithoutFailing() {
    Parcel parcel = parcel("banana");
    when(parcelRepository.findAll()).thenReturn(List.of(parcel));
    when(parcelRepository.getReferenceById(parcel.getId())).thenReturn(parcel);
    when(parcelRiskRepository.findById(any())).thenReturn(Optional.empty());
    when(aiServiceClient.getRisk(any(RiskRequest.class)))
        .thenReturn(
            new app.aiclient.RiskResponse(
                "banana",
                "not_modelled",
                0,
                "No weather model for this crop.",
                List.of(),
                Instant.parse("2026-11-20T06:00:00Z")));

    int stored = service.refreshAllParcels();

    assertThat(stored).isEqualTo(1);
    ArgumentCaptor<ParcelRisk> saved = ArgumentCaptor.forClass(ParcelRisk.class);
    verify(parcelRiskRepository).save(saved.capture());
    assertThat(saved.getValue().getLevel()).isEqualTo("not_modelled");
    assertThat(saved.getValue().getRiskIndex()).isZero();
    assertThat(saved.getValue().getDiseases()).isEmpty();
    verify(alertPublisher, never()).publish(any(AlertMessage.class));
  }

  @Test
  void aFailedParcelDoesNotStopOthersOrOverwriteItsLastGoodResult() {
    Parcel failing = parcel("potato");
    Parcel healthy = parcel("tomato");
    when(parcelRepository.findAll()).thenReturn(List.of(failing, healthy));
    when(parcelRepository.getReferenceById(healthy.getId())).thenReturn(healthy);
    when(parcelRiskRepository.findById(any())).thenReturn(Optional.empty());
    when(aiServiceClient.getRisk(any(RiskRequest.class)))
        .thenThrow(new AiServiceException("AI down"))
        .thenReturn(
            new app.aiclient.RiskResponse(
                "tomato",
                "high",
                78,
                "High risk.",
                List.of(new DiseaseRisk("late_blight", "tomcast", "high", 78)),
                Instant.parse("2026-11-20T06:00:00Z")));

    int stored = service.refreshAllParcels();

    assertThat(stored).isEqualTo(1);
    // The failing parcel is never written, so its previous stored result is untouched.
    verify(parcelRepository, never()).getReferenceById(failing.getId());
    ArgumentCaptor<ParcelRisk> saved = ArgumentCaptor.forClass(ParcelRisk.class);
    verify(parcelRiskRepository).save(saved.capture());
    assertThat(saved.getValue().getParcel().getId()).isEqualTo(healthy.getId());
    assertThat(saved.getValue().getLevel()).isEqualTo("high");
    ArgumentCaptor<AlertMessage> alert = ArgumentCaptor.forClass(AlertMessage.class);
    verify(alertPublisher).publish(alert.capture());
    assertThat(alert.getValue().userId()).isEqualTo(healthy.getOwner().getId());
    assertThat(alert.getValue().recipientEmail()).isEqualTo(healthy.getOwner().getEmail());
    assertThat(alert.getValue().parcelName()).isEqualTo("Field");
    assertThat(alert.getValue().crop()).isEqualTo("tomato");
    assertThat(alert.getValue().riskIndex()).isEqualTo(78);
    assertThat(alert.getValue().reason()).isEqualTo("High risk.");
    assertThat(alert.getValue().generatedAt()).isEqualTo(Instant.parse("2026-11-20T06:00:00Z"));
  }

  @Test
  void alertPublicationFailureDoesNotInterruptRiskStorage() {
    Parcel parcel = parcel("tomato");
    when(parcelRepository.findAll()).thenReturn(List.of(parcel));
    when(parcelRepository.getReferenceById(parcel.getId())).thenReturn(parcel);
    when(parcelRiskRepository.findById(any())).thenReturn(Optional.empty());
    when(aiServiceClient.getRisk(any(RiskRequest.class)))
        .thenReturn(
            new app.aiclient.RiskResponse(
                "tomato",
                "high",
                78,
                "High risk.",
                List.of(),
                Instant.parse("2026-11-20T06:00:00Z")));
    org.mockito.Mockito.doThrow(new IllegalStateException("RabbitMQ unavailable"))
        .when(alertPublisher)
        .publish(any(AlertMessage.class));

    int stored = service.refreshAllParcels();

    assertThat(stored).isEqualTo(1);
    verify(parcelRiskRepository).save(any(ParcelRisk.class));
  }

  private static Parcel parcel(String crop) {
    UUID ownerId = UUID.randomUUID();
    return new Parcel(
        UUID.randomUUID(),
        new UserAccount(ownerId, "owner-" + ownerId + "@example.com", "hash", Instant.now()),
        "Field",
        crop,
        squarePolygon(),
        Instant.now());
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
    polygon.setSRID(4326);
    return polygon;
  }
}
