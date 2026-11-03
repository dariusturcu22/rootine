package app.aiclient;

import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

/** HTTP client for the AI service. */
@Component
public class AiServiceClient {

  private final RestClient restClient;

  public AiServiceClient(RestClient restClient) {
    this.restClient = restClient;
  }

  public HealthResponse getHealth() {
    return execute(() -> restClient.get().uri("/health").retrieve().body(HealthResponse.class));
  }

  public List<CropResponse> getCrops() {
    return execute(
        () ->
            restClient
                .get()
                .uri("/v1/crops")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {}));
  }

  public PredictionResponse predict(MultipartFile image, String crop) {
    return execute(
        () -> {
          Resource resource = image.getResource();
          HttpHeaders imageHeaders = new HttpHeaders();
          imageHeaders.setContentDispositionFormData("image", image.getOriginalFilename());
          if (image.getContentType() != null) {
            imageHeaders.setContentType(MediaType.parseMediaType(image.getContentType()));
          }
          MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
          body.add("image", new HttpEntity<>(resource, imageHeaders));
          if (crop != null) {
            body.add("crop", crop);
          }
          return restClient
              .post()
              .uri("/v1/predict")
              .contentType(MediaType.MULTIPART_FORM_DATA)
              .body(body)
              .retrieve()
              .body(PredictionResponse.class);
        });
  }

  public RiskResponse getRisk(RiskRequest request) {
    return execute(
        () ->
            restClient
                .post()
                .uri("/v1/risk")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(RiskResponse.class));
  }

  private static <T> T execute(SupplierWithException<T> operation) {
    try {
      T response = operation.get();
      if (response == null) {
        throw new AiServiceException("AI service returned an empty response");
      }
      return response;
    } catch (AiServiceException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new AiServiceException("AI service request failed", exception);
    }
  }

  @FunctionalInterface
  private interface SupplierWithException<T> {
    T get();
  }
}
