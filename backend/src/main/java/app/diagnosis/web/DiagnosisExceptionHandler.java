package app.diagnosis.web;

import app.diagnosis.service.DiagnosisFileTooLargeException;
import app.diagnosis.service.DiagnosisNotFoundException;
import app.diagnosis.service.InvalidDiagnosisUploadException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class DiagnosisExceptionHandler {

  @ExceptionHandler(InvalidDiagnosisUploadException.class)
  ProblemDetail handleInvalidUpload(InvalidDiagnosisUploadException exception) {
    return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(DiagnosisNotFoundException.class)
  ProblemDetail handleNotFound(DiagnosisNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ProblemDetail handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
    return problem(
        HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file must not be larger than 10 MB.");
  }

  @ExceptionHandler(DiagnosisFileTooLargeException.class)
  ProblemDetail handleDiagnosisFileTooLarge(DiagnosisFileTooLargeException exception) {
    return problem(HttpStatus.PAYLOAD_TOO_LARGE, exception.getMessage());
  }

  @ExceptionHandler(MultipartException.class)
  ProblemDetail handleMalformedMultipart(MultipartException exception) {
    return problem(HttpStatus.BAD_REQUEST, "The multipart request is invalid.");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ProblemDetail handleInvalidArgument(MethodArgumentTypeMismatchException exception) {
    return problem(HttpStatus.BAD_REQUEST, "A request parameter has an invalid value.");
  }

  private static ProblemDetail problem(HttpStatus status, String detail) {
    return ProblemDetail.forStatusAndDetail(status, detail);
  }
}
