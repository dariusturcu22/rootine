package app.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.Application;
import app.aiclient.AiServiceClient;
import app.aiclient.AiServiceException;
import app.common.config.OpenApiConfig;
import app.common.error.ProblemDetailResponse;
import app.diagnosis.service.DiagnosisService;
import app.parcel.dto.ParcelRequest;
import app.parcel.dto.ParcelResponse;
import app.parcel.dto.PolygonGeometry;
import app.parcel.service.ParcelNotFoundException;
import app.parcel.service.ParcelService;
import app.risk.dto.DiseaseRisk;
import app.risk.dto.RiskLevel;
import app.risk.dto.RiskResponse;
import app.risk.service.RiskService;
import app.user.dto.LoginResponse;
import app.user.service.DemoTokenService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Checks demo endpoint behavior and OpenAPI paths without needing a database or a message broker.
 * The frontend generates its API types from {@code /v3/api-docs}.
 *
 * <p>This is a unit test ({@code *Test}, surefire), so it runs without Docker. Data, messaging and
 * mail autoconfigurations are excluded; persistence-dependent services are mocked here.
 */
@SpringBootTest(
    classes = Application.class,
    properties = {
      "spring.autoconfigure.exclude=" + ContractEndpointsTest.EXCLUDED_AUTOCONFIG,
      "app.demo-token.enabled=true"
    })
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ContractEndpointsTest {

  /**
   * Autoconfigurations the mocked controllers do not need. Excluding them lets the OpenAPI document
   * build without a database or a message broker.
   */
  static final String EXCLUDED_AUTOCONFIG =
      "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
          + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
          + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
          + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
          + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration,"
          + "org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration";

  private static final String PROBLEM_JSON = ProblemDetailResponse.MEDIA_TYPE;

  /** Every path from the contract, exactly as it appears in the OpenAPI document. */
  private static final List<String> CONTRACT_PATHS =
      List.of(
          "/auth/register",
          "/auth/login",
          "/auth/demo-token",
          "/me",
          "/crops",
          "/parcels",
          "/parcels/{id}",
          "/parcels/{id}/risk",
          "/diagnoses",
          "/diagnoses/{id}",
          "/risk/heatmap",
          "/alerts/stream");

  @Autowired private MockMvc mockMvc;
  @MockitoBean private DiagnosisService diagnosisService;
  @MockitoBean private AiServiceClient aiServiceClient;
  @MockitoBean private ParcelService parcelService;
  @MockitoBean private RiskService riskService;
  @MockitoBean private DemoTokenService demoTokenService;

  @Test
  void demoTokenEndpointReturnsTheConfiguredTokenResponse() throws Exception {
    given(demoTokenService.issueToken())
        .willReturn(new LoginResponse("demo.jwt.token", "Bearer", 3600));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/auth/demo-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("demo.jwt.token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(3600));
  }

  @Test
  void cropEndpointReturnsTheAiServiceCropList() throws Exception {
    given(aiServiceClient.getCrops())
        .willReturn(
            List.of(
                new app.aiclient.CropResponse(
                    "potato",
                    true,
                    true,
                    "lab_only",
                    List.of("hutton"),
                    List.of("early_blight", "healthy", "late_blight"))));

    mockMvc
        .perform(get("/crops").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].crop").value("potato"))
        .andExpect(jsonPath("$[0].supportLevel").value("lab_only"))
        .andExpect(jsonPath("$[0].classes[2]").value("late_blight"));
  }

  @Test
  void cropEndpointMapsAiServiceFailuresTo502ProblemDetails() throws Exception {
    given(aiServiceClient.getCrops()).willThrow(new AiServiceException("unavailable"));

    mockMvc
        .perform(get("/crops").with(jwt()))
        .andExpect(status().isBadGateway())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(502));
  }

  @Test
  void parcelRiskEndpointReturnsTheLiveRiskForTheAuthenticatedOwner() throws Exception {
    UUID ownerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    UUID parcelId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    given(riskService.getParcelRisk(ownerId, parcelId))
        .willReturn(
            new RiskResponse(
                parcelId,
                "potato",
                RiskLevel.MEDIUM,
                42,
                "Mock risk",
                List.of(new DiseaseRisk("late_blight", "hutton", RiskLevel.MEDIUM, 42)),
                Instant.parse("2026-11-09T10:00:00Z")));

    mockMvc
        .perform(
            get("/parcels/{id}/risk", parcelId)
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parcelId").value(parcelId.toString()))
        .andExpect(jsonPath("$.level").value("medium"))
        .andExpect(jsonPath("$.diseases[0].riskIndex").value(42));
  }

  @Test
  void parcelRiskEndpointHidesUnknownOrNonOwnedParcels() throws Exception {
    UUID ownerId = UUID.randomUUID();
    UUID parcelId = UUID.randomUUID();
    given(riskService.getParcelRisk(ownerId, parcelId)).willThrow(new ParcelNotFoundException());

    mockMvc
        .perform(
            get("/parcels/{id}/risk", parcelId)
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void parcelRiskEndpointMapsAiFailuresTo502ProblemDetails() throws Exception {
    UUID ownerId = UUID.randomUUID();
    UUID parcelId = UUID.randomUUID();
    given(riskService.getParcelRisk(ownerId, parcelId))
        .willThrow(new AiServiceException("unavailable"));

    mockMvc
        .perform(
            get("/parcels/{id}/risk", parcelId)
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isBadGateway())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(502));
  }

  @Test
  void parcelEndpointsCreateAndListTheAuthenticatedUsersParcels() throws Exception {
    UUID ownerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    UUID parcelId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    ParcelResponse response =
        new ParcelResponse(
            parcelId,
            "North field",
            "potato",
            new PolygonGeometry(
                "Polygon",
                List.of(
                    List.of(
                        List.of(23.59, 46.77),
                        List.of(23.60, 46.77),
                        List.of(23.60, 46.78),
                        List.of(23.59, 46.78),
                        List.of(23.59, 46.77)))),
            Instant.parse("2026-11-01T08:00:00Z"));
    given(parcelService.createParcel(eq(ownerId), any(ParcelRequest.class))).willReturn(response);
    given(parcelService.listParcels(ownerId)).willReturn(List.of(response));

    String body =
        """
        {"name":"North field","crop":"potato","geometry":{"type":"Polygon","coordinates":[[[23.59,46.77],[23.60,46.77],[23.60,46.78],[23.59,46.78],[23.59,46.77]]]}}
        """;
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/parcels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(parcelId.toString()))
        .andExpect(jsonPath("$.geometry.coordinates[0][0][0]").value(23.59));

    mockMvc
        .perform(get("/parcels").with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(parcelId.toString()));

    given(parcelService.replaceParcel(eq(ownerId), eq(parcelId), any(ParcelRequest.class)))
        .willReturn(response);
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/parcels/{id}", parcelId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(parcelId.toString()));
  }

  @Test
  void parcelEndpointsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/parcels")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/crops")).andExpect(status().isUnauthorized());
  }

  @Test
  void openApiDocumentListsEveryContractPath() throws Exception {
    var resultActions =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

    for (String contractPath : CONTRACT_PATHS) {
      resultActions.andExpect(jsonPath("$.paths['" + contractPath + "']").exists());
    }
  }

  @Test
  void openApiDocumentDeclaresTheBearerSecurityScheme() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.components.securitySchemes."
                        + OpenApiConfig.BEARER_AUTH_SCHEME_NAME
                        + ".scheme")
                .value("bearer"));
  }
}
