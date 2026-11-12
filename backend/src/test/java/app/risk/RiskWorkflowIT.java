package app.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.AbstractIntegrationTest;
import app.aiclient.AiServiceClient;
import app.aiclient.AiServiceException;
import app.aiclient.RiskRequest;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class RiskWorkflowIT extends AbstractIntegrationTest {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), SpatialReference.WGS84_SRID);
  private static final String PROBLEM_JSON = "application/problem+json";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private ParcelRepository parcelRepository;
  @MockitoBean private AiServiceClient aiServiceClient;

  @Test
  void requestsLiveRiskAtParcelCenterAndReturnsTheResult() throws Exception {
    UUID ownerId = UUID.randomUUID();
    Parcel parcel = savedParcel(ownerId);
    when(aiServiceClient.getRisk(any()))
        .thenReturn(
            new app.aiclient.RiskResponse(
                "potato",
                "medium",
                42,
                "Conditions favor late blight.",
                List.of(new app.aiclient.DiseaseRisk("late_blight", "hutton", "medium", 42)),
                Instant.parse("2026-11-11T12:00:00Z")));

    mockMvc
        .perform(
            get("/parcels/{id}/risk", parcel.getId())
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parcelId").value(parcel.getId().toString()))
        .andExpect(jsonPath("$.crop").value("potato"))
        .andExpect(jsonPath("$.level").value("medium"))
        .andExpect(jsonPath("$.reason").value("Conditions favor late blight."))
        .andExpect(jsonPath("$.diseases[0].model").value("hutton"));

    org.mockito.ArgumentCaptor<RiskRequest> request =
        org.mockito.ArgumentCaptor.forClass(RiskRequest.class);
    verify(aiServiceClient).getRisk(request.capture());
    assertThat(request.getValue().latitude())
        .isCloseTo(46.775, org.assertj.core.data.Offset.offset(0.0001));
    assertThat(request.getValue().longitude())
        .isCloseTo(23.595, org.assertj.core.data.Offset.offset(0.0001));
    assertThat(request.getValue().crop()).isEqualTo("potato");
  }

  @Test
  void hidesParcelsOwnedByAnotherUserFromRiskEndpoint() throws Exception {
    UUID ownerId = UUID.randomUUID();
    UUID otherOwnerId = UUID.randomUUID();
    Parcel parcel = savedParcel(ownerId);

    mockMvc
        .perform(
            get("/parcels/{id}/risk", parcel.getId())
                .with(jwt().jwt(token -> token.subject(otherOwnerId.toString()))))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404));

    verifyNoInteractions(aiServiceClient);
  }

  @Test
  void mapsRiskServiceFailureToProblemDetails() throws Exception {
    UUID ownerId = UUID.randomUUID();
    Parcel parcel = savedParcel(ownerId);
    when(aiServiceClient.getRisk(any())).thenThrow(new AiServiceException("unavailable"));

    mockMvc
        .perform(
            get("/parcels/{id}/risk", parcel.getId())
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isBadGateway())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(502))
        .andExpect(jsonPath("$.title").value("Bad Gateway"));
  }

  private Parcel savedParcel(UUID ownerId) {
    UserAccount owner =
        userRepository.save(
            new UserAccount(
                ownerId,
                "risk-owner-" + ownerId + "@example.com",
                "test-hash",
                Instant.now().truncatedTo(ChronoUnit.MILLIS)));
    Coordinate[] ring = {
      new Coordinate(23.59, 46.77),
      new Coordinate(23.60, 46.77),
      new Coordinate(23.60, 46.78),
      new Coordinate(23.59, 46.78),
      new Coordinate(23.59, 46.77)
    };
    Polygon polygon = GEOMETRY_FACTORY.createPolygon(ring);
    polygon.setSRID(SpatialReference.WGS84_SRID);
    return parcelRepository.save(
        new Parcel(
            UUID.randomUUID(),
            owner,
            "North field",
            "potato",
            polygon,
            Instant.now().truncatedTo(ChronoUnit.MILLIS)));
  }
}
