package app.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.AbstractIntegrationTest;
import app.common.error.ProblemDetailResponse;
import app.user.repository.UserRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end auth flow against the real database: register, log in, read the current user. */
@AutoConfigureMockMvc
class AuthWorkflowIT extends AbstractIntegrationTest {

  private static final String PROBLEM_JSON = ProblemDetailResponse.MEDIA_TYPE;

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private JwtDecoder jwtDecoder;

  @Test
  void registersLogsInAndReadsTheCurrentUser() throws Exception {
    String credentials = "{\"email\":\"Ana@Example.com\",\"password\":\"a-strong-password\"}";

    String created =
        mockMvc
            .perform(
                post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("ana@example.com"))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String userId = JsonMapper.builder().build().readTree(created).get("id").asText();

    // The stored password must be a hash, never the plaintext.
    String storedHash =
        userRepository.findByEmail("ana@example.com").orElseThrow().getPasswordHash();
    assertThat(storedHash).isNotEqualTo("a-strong-password");
    assertThat(storedHash).startsWith("$2");

    String loginBody =
        mockMvc
            .perform(
                post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(3600))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String accessToken =
        JsonMapper.builder().build().readTree(loginBody).get("accessToken").asText();

    var jwt = jwtDecoder.decode(accessToken);
    assertThat(jwt.getSubject()).isEqualTo(userId);
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds())
        .isEqualTo(3600);

    mockMvc
        .perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.email").value("ana@example.com"));
  }

  @Test
  void rejectsDuplicateEmailWithConflict() throws Exception {
    String credentials = "{\"email\":\"dup@example.com\",\"password\":\"a-strong-password\"}";
    mockMvc
        .perform(
            post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"Dup@Example.com\",\"password\":\"another-password\"}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void rejectsInvalidRegistrationInputWithUnprocessableEntity() throws Exception {
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(422));
  }

  @Test
  void rejectsWrongPasswordWithUnauthorized() throws Exception {
    String credentials = "{\"email\":\"wrong@example.com\",\"password\":\"a-strong-password\"}";
    mockMvc
        .perform(
            post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"wrong@example.com\",\"password\":\"bad-password\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void rejectsUnknownEmailLoginWithUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\",\"password\":\"a-strong-password\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void rejectsMissingTokenOnCurrentUser() throws Exception {
    mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
  }
}
