package app.parcel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.AbstractIntegrationTest;
import app.parcel.repository.ParcelRepository;
import app.user.domain.UserAccount;
import app.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ParcelWorkflowIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private ParcelRepository parcelRepository;

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

  private UUID createUser() {
    UUID id = UUID.randomUUID();
    userRepository.save(
        new UserAccount(id, "parcel-owner-" + id + "@example.com", "test-hash", Instant.now()));
    return id;
  }
}
