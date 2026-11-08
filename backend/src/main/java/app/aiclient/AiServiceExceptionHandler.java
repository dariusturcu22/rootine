package app.aiclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AiServiceExceptionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(AiServiceExceptionHandler.class);

  @ExceptionHandler(AiServiceException.class)
  ProblemDetail handleAiServiceException(AiServiceException exception) {
    LOGGER.warn("AI service request failed: {}", exception.getMessage(), exception);
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_GATEWAY, "The AI service is unavailable or returned an error.");
    problemDetail.setTitle("Bad Gateway");
    return problemDetail;
  }
}
