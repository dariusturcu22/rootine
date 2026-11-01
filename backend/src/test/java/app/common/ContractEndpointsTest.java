package app.common;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.Application;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Checks the two promises of the contract endpoint stubs without needing a database or a message
 * broker: every published endpoint that is not implemented yet returns a 501 Problem Details
 * response, and the OpenAPI document at {@code /v3/api-docs} lists every path from {@code
 * docs/api/backend.md}. The frontend generates its API types from that document.
 *
 * <p>This is a unit test ({@code *Test}, surefire), so it runs without Docker. The data, messaging
 * and mail autoconfigurations are excluded because the stub controllers do not touch them yet.
 */
@SpringBootTest(
    classes = Application.class,
    properties = {"spring.autoconfigure.exclude=" + ContractEndpointsTest.EXCLUDED_AUTOCONFIG})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ContractEndpointsTest {

  /**
   * Autoconfigurations the stub controllers do not need yet. Excluding them lets the OpenAPI
   * document build without a database or a message broker.
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

  @Test
  void unimplementedEndpointReturnsNotImplementedProblemDetails() throws Exception {
    mockMvc
        .perform(get("/crops").with(jwt()))
        .andExpect(status().isNotImplemented())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(501))
        .andExpect(jsonPath("$.title").value("Not Implemented"))
        .andExpect(jsonPath("$.detail").value(EndpointStub.NOT_IMPLEMENTED_DETAIL));
  }

  @Test
  void anotherUnimplementedEndpointAlsoReturnsNotImplemented() throws Exception {
    mockMvc
        .perform(get("/parcels").with(jwt()))
        .andExpect(status().isNotImplemented())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(501));
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
