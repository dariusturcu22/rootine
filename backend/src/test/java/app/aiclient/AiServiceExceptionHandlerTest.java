package app.aiclient;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class AiServiceExceptionHandlerTest {

  @Test
  void returnsBadGatewayForAiServiceFailure() {
    ProblemDetail response =
        new AiServiceExceptionHandler()
            .handleAiServiceException(new AiServiceException("upstream"));

    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
  }
}
