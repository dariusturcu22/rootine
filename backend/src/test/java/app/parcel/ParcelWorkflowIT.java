package app.parcel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.AbstractIntegrationTest;
import app.aiclient.AiServiceClient;
import app.aiclient.AiServiceException;
import app.aiclient.DiseaseRisk;
import app.parcel.repository.ParcelRepository;
import app.user.domain.UserAccount;
import app.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ParcelWorkflowIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private ParcelRepository parcelRepository;
  @MockitoBean private AiServiceClient aiServiceClient;

  @Test
  void rejectsInvalidBearerTokenWithUnauthorized() throws Exception {
    mockMvc
        .perform(get("/parcels").header("Authorization", "Bearer invalid.token.value"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createsAndListsOnlyTheAuthenticatedUsersParcels() throws Exception {
    UUID ownerId = createUser();
    UUID otherOwnerId = createUser();
    String request =
        """
        {"name":"North field","crop":"potato","geometry":{"type":"Polygon","coordinates":[[[23.59,46.77],[23.60,46.77],[23.60,46.78],[23.59,46.78],[23.59,46.77]]]}}
        """;

    String created =
        mockMvc
            .perform(
                post("/parcels")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
                    .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("North field"))
            .andExpect(jsonPath("$.crop").value("potato"))
            .andExpect(jsonPath("$.geometry.type").value("Polygon"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID parcelId =
        com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .build()
            .readTree(created)
            .get("id")
            .asText()
            .transform(UUID::fromString);

    assertThat(parcelRepository.findByIdAndOwnerId(parcelId, ownerId)).isPresent();

    mockMvc
        .perform(get("/parcels").with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(parcelId.toString()));

    mockMvc
        .perform(get("/parcels").with(jwt().jwt(token -> token.subject(otherOwnerId.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void rejectsInvalidPolygonsAndInvalidParcelFieldsWithProblemDetails() throws Exception {
    UUID ownerId = createUser();
    String selfIntersecting =
        """
        {"name":"Invalid field","crop":"potato","geometry":{"type":"Polygon","coordinates":[[[0,0],[1,1],[0,1],[1,0],[0,0]]]}}
        """;
    mockMvc
        .perform(
            post("/parcels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(selfIntersecting)
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value(422));

    String blankName =
        """
        {"name":" ","crop":"potato","geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,1],[0,0]]]}}
        """;
    mockMvc
        .perform(
            post("/parcels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(blankName)
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value(422));
  }

  @Test
  void replacesOnlyTheAuthenticatedUsersParcel() throws Exception {
    UUID ownerId = createUser();
    UUID otherOwnerId = createUser();
    String originalRequest =
        """
        {"name":"North field","crop":"potato","geometry":{"type":"Polygon","coordinates":[[[23.59,46.77],[23.60,46.77],[23.60,46.78],[23.59,46.78],[23.59,46.77]]]}}
        """;
    String replacementRequest =
        """
        {"name":"Updated field","crop":"tomato","geometry":{"type":"Polygon","coordinates":[[[24.59,47.77],[24.60,47.77],[24.60,47.78],[24.59,47.78],[24.59,47.77]]]}}
        """;
    String created =
        mockMvc
            .perform(
                post("/parcels")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(originalRequest)
                    .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID parcelId =
        com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .build()
            .readTree(created)
            .get("id")
            .asText()
            .transform(UUID::fromString);

    mockMvc
        .perform(
            put("/parcels/{id}", parcelId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(replacementRequest)
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(parcelId.toString()))
        .andExpect(jsonPath("$.name").value("Updated field"))
        .andExpect(jsonPath("$.crop").value("tomato"))
        .andExpect(jsonPath("$.geometry.coordinates[0][0][0]").value(24.59));

    mockMvc
        .perform(
            put("/parcels/{id}", parcelId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(replacementRequest)
                .with(jwt().jwt(token -> token.subject(otherOwnerId.toString()))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void returnsLiveRiskOnlyForTheParcelOwnerAndMapsAiFailures() throws Exception {
    UUID ownerId = createUser();
    UUID otherOwnerId = createUser();
    String request =
        """
        {"name":"Risk field","crop":"potato","geometry":{"type":"Polygon","coordinates":[[[23.59,46.77],[23.60,46.77],[23.60,46.78],[23.59,46.78],[23.59,46.77]]]}}
        """;
    String created =
        mockMvc
            .perform(
                post("/parcels")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
                    .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID parcelId =
        com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .build()
            .readTree(created)
            .get("id")
            .asText()
            .transform(UUID::fromString);

    given(aiServiceClient.getRisk(any()))
        .willReturn(
            new app.aiclient.RiskResponse(
                "potato",
                "medium",
                42,
                "Rain and temperature conditions.",
                List.of(new DiseaseRisk("late_blight", "hutton", "medium", 42)),
                Instant.parse("2026-11-09T10:00:00Z")));

    mockMvc
        .perform(
            get("/parcels/{id}/risk", parcelId)
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parcelId").value(parcelId.toString()))
        .andExpect(jsonPath("$.level").value("medium"))
        .andExpect(jsonPath("$.riskIndex").value(42));

    mockMvc
        .perform(
            get("/parcels/{id}/risk", parcelId)
                .with(jwt().jwt(token -> token.subject(otherOwnerId.toString()))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));

    given(aiServiceClient.getRisk(any())).willThrow(new AiServiceException("unavailable"));
    mockMvc
        .perform(
            get("/parcels/{id}/risk", parcelId)
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.status").value(502));
  }

  private UUID createUser() {
    UUID id = UUID.randomUUID();
    userRepository.save(
        new UserAccount(id, "parcel-owner-" + id + "@example.com", "test-hash", Instant.now()));
    return id;
  }
}
