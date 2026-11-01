package app.aiclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiServiceClientTest {

  private static final String BASE_URL = "http://ai-service.test";

  private MockRestServiceServer server;
  private AiServiceClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    server = MockRestServiceServer.bindTo(builder).build();
    client = new AiServiceClient(builder.build());
  }

  @Test
  void getsHealthAndMapsModelVersion() {
    server
        .expect(requestTo(BASE_URL + "/health"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"status\":\"ok\",\"model_version\":\"plant-v1\"}", MediaType.APPLICATION_JSON));

    HealthResponse response = client.getHealth();

    assertThat(response.status()).isEqualTo("ok");
    assertThat(response.modelVersion()).isEqualTo("plant-v1");
    server.verify();
  }

  @Test
  void getsCropsAndMapsSnakeCaseFields() {
    server
        .expect(requestTo(BASE_URL + "/v1/crops"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "[{\"crop\":\"potato\",\"diagnosis_supported\":true,\"weather_supported\":true,"
                    + "\"support_level\":\"lab_only\",\"weather_rules\":[\"hutton\"],"
                    + "\"classes\":[\"early_blight\",\"healthy\"]}]",
                MediaType.APPLICATION_JSON));

    var response = client.getCrops();

    assertThat(response).hasSize(1);
    assertThat(response.get(0).diagnosisSupported()).isTrue();
    assertThat(response.get(0).weatherSupported()).isTrue();
    assertThat(response.get(0).supportLevel()).isEqualTo("lab_only");
    assertThat(response.get(0).weatherRules()).containsExactly("hutton");
    assertThat(response.get(0).classes()).containsExactly("early_blight", "healthy");
    server.verify();
  }

  @Test
  void postsMultipartPrediction() {
    server
        .expect(requestTo(BASE_URL + "/v1/predict"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"image\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"crop\"")))
        .andRespond(
            withSuccess(
                "{\"status\":\"confident\",\"crop\":\"potato\",\"crop_check\":\"ok\","
                    + "\"top_predictions\":[{\"label\":\"potato_early_blight\","
                    + "\"disease\":\"early_blight\",\"probability\":0.92}],"
                    + "\"support_level\":\"lab_only\",\"model_version\":\"plant-v1\"}",
                MediaType.APPLICATION_JSON));

    var image = new MockMultipartFile("image", "leaf.jpg", "image/jpeg", new byte[] {1, 2, 3});
    PredictionResponse response = client.predict(image, "potato");

    assertThat(response.status()).isEqualTo("confident");
    assertThat(response.cropCheck()).isEqualTo("ok");
    assertThat(response.topPredictions()).hasSize(1);
    assertThat(response.topPredictions().get(0).probability()).isEqualTo(0.92);
    assertThat(response.supportLevel()).isEqualTo("lab_only");
    assertThat(response.modelVersion()).isEqualTo("plant-v1");
    server.verify();
  }

  @Test
  void omitsOptionalCropFromPrediction() {
    server
        .expect(requestTo(BASE_URL + "/v1/predict"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("name=\"crop\""))))
        .andRespond(
            withSuccess(
                "{\"status\":\"confident\",\"crop\":\"tomato\",\"crop_check\":null,"
                    + "\"top_predictions\":[],\"support_level\":\"lab_only\","
                    + "\"model_version\":\"plant-v1\"}",
                MediaType.APPLICATION_JSON));

    client.predict(new MockMultipartFile("image", "leaf.jpg", "image/jpeg", new byte[] {1}), null);

    server.verify();
  }

  @Test
  void postsRiskWithSnakeCaseReplayDateAndMapsResponse() {
    server
        .expect(requestTo(BASE_URL + "/v1/risk"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    "{\"latitude\":46.77,\"longitude\":23.59,\"crop\":\"potato\","
                        + "\"replay_date\":\"2026-06-20\"}"))
        .andRespond(
            withSuccess(
                "{\"crop\":\"potato\",\"level\":\"medium\",\"risk_index\":42,"
                    + "\"reason\":\"Mock risk\",\"diseases\":[{\"disease\":\"late_blight\","
                    + "\"model\":\"hutton\",\"level\":\"medium\",\"risk_index\":42}],"
                    + "\"generated_at\":\"2026-11-01T06:00:00Z\"}",
                MediaType.APPLICATION_JSON));

    RiskResponse response =
        client.getRisk(new RiskRequest(46.77, 23.59, "potato", LocalDate.parse("2026-06-20")));

    assertThat(response.level()).isEqualTo("medium");
    assertThat(response.riskIndex()).isEqualTo(42);
    assertThat(response.reason()).isEqualTo("Mock risk");
    assertThat(response.diseases()).hasSize(1);
    assertThat(response.diseases().get(0).riskIndex()).isEqualTo(42);
    assertThat(response.generatedAt()).isEqualTo(Instant.parse("2026-11-01T06:00:00Z"));
    server.verify();
  }

  @Test
  void omitsOptionalReplayDateFromRiskRequest() {
    server
        .expect(requestTo(BASE_URL + "/v1/risk"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("replay_date"))))
        .andRespond(
            withSuccess(
                "{\"crop\":\"potato\",\"level\":\"not_modelled\",\"risk_index\":0,"
                    + "\"reason\":\"No model\",\"diseases\":[],"
                    + "\"generated_at\":\"2026-11-01T06:00:00Z\"}",
                MediaType.APPLICATION_JSON));

    client.getRisk(new RiskRequest(46.77, 23.59, "potato", null));

    server.verify();
  }

  @Test
  void mapsNonMockNotImplementedResponseToAiServiceException() {
    server
        .expect(requestTo(BASE_URL + "/v1/risk"))
        .andRespond(withStatus(HttpStatus.NOT_IMPLEMENTED));

    assertThatThrownBy(() -> client.getRisk(new RiskRequest(46.77, 23.59, "potato", null)))
        .isInstanceOf(AiServiceException.class);
    server.verify();
  }

  @Test
  void mapsMalformedResponseToAiServiceException() {
    server
        .expect(requestTo(BASE_URL + "/health"))
        .andRespond(withSuccess("{invalid", MediaType.APPLICATION_JSON));

    assertThatThrownBy(client::getHealth).isInstanceOf(AiServiceException.class);
    server.verify();
  }
}
