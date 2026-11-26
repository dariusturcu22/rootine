package app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ApplicationIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void healthEndpointIsUpAndPublic() throws Exception {
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void otherEndpointsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/anything")).andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsInvalidBearerTokenWithProblemDetailsAndUnauthorized() throws Exception {
    mockMvc
        .perform(get("/parcels").header("Authorization", "Bearer invalid.token.value"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.detail").value("A valid bearer token is required."));
  }
}
