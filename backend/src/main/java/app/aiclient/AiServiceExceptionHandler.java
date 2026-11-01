package app.aiclient;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AiServiceExceptionHandler {

  @ExceptionHandler(AiServiceException.class)
  ProblemDetail handleAiServiceException(AiServiceException exception) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_GATEWAY, "The AI service request failed");
  }
}
