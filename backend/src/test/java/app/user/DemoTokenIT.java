package app.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.AbstractIntegrationTest;
import app.user.repository.UserRepository;
import app.user.service.DemoTokenService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(properties = "app.demo-token.enabled=true")
class DemoTokenIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtDecoder jwtDecoder;
  @Autowired private UserRepository userRepository;

  @Test
  void issuesOneHourTokenForPersistedDemoUserThatCanAccessProtectedEndpoints() throws Exception {
    String token =
        mockMvc
            .perform(post("/auth/demo-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(3600))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String accessToken =
        com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .build()
            .readTree(token)
            .get("accessToken")
            .asText();

    var jwt = jwtDecoder.decode(accessToken);
    assertThat(jwt.getSubject()).isEqualTo(DemoTokenService.DEMO_USER_ID.toString());
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds())
        .isEqualTo(3600);
    assertThat(userRepository.findById(DemoTokenService.DEMO_USER_ID)).isPresent();

    mockMvc
        .perform(get("/parcels").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }
}
