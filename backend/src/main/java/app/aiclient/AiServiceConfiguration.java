package app.aiclient;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiServiceConfiguration {

  @Bean
  RestClient aiServiceRestClient(
      @Value("${app.ai-service-url}") String baseUrl,
      @Value("${app.ai-service-connect-timeout:2s}") Duration connectTimeout,
      @Value("${app.ai-service-read-timeout:30s}") Duration readTimeout) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);
    return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
  }
}
