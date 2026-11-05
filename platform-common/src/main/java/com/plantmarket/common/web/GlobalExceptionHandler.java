package com.plantmarket.common.web;

import com.plantmarket.common.error.ApiException;
import com.plantmarket.common.error.FieldViolation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps exceptions to RFC 7807 problem responses with a request correlation id. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ProblemDetail> handleApiException(
      ApiException exception, HttpServletRequest request) {
    ProblemDetail problem =
        problem(
            exception.getStatus(),
            exception.getProblemType(),
            exception.getMessage(),
            request,
            exception.getViolations());
    return ResponseEntity.status(exception.getStatus())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<FieldViolation> violations =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
            .toList();
    ProblemDetail problem =
        problem(
            HttpStatus.BAD_REQUEST,
            ApiException.PROBLEM_BASE + "validation",
            "Validation failed",
            request,
            violations);
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(
      ConstraintViolationException exception, HttpServletRequest request) {
    List<FieldViolation> violations =
        exception.getConstraintViolations().stream().map(this::toFieldViolation).toList();
    ProblemDetail problem =
        problem(
            HttpStatus.BAD_REQUEST,
            ApiException.PROBLEM_BASE + "validation",
            "Validation failed",
            request,
            violations);
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @ExceptionHandler(ErrorResponseException.class)
  public ResponseEntity<ProblemDetail> handleErrorResponse(
      ErrorResponseException exception, HttpServletRequest request) {
    ProblemDetail problem = exception.getBody();
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("traceId", RequestIdFilter.current(request));
    return ResponseEntity.status(exception.getStatusCode())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ProblemDetail> handleMalformedRequest(
      Exception exception, HttpServletRequest request) {
    ProblemDetail problem =
        problem(
            HttpStatus.BAD_REQUEST,
            ApiException.PROBLEM_BASE + "bad-request",
            "Malformed request",
            request,
            List.of());
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    log.error("Unhandled exception for {}", request.getRequestURI(), exception);
    ProblemDetail problem =
        problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ApiException.PROBLEM_BASE + "internal",
            "Unexpected server error",
            request,
            List.of());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  private FieldViolation toFieldViolation(ConstraintViolation<?> violation) {
    return new FieldViolation(violation.getPropertyPath().toString(), violation.getMessage());
  }

  private ProblemDetail problem(
      HttpStatusCode status,
      String type,
      String detail,
      HttpServletRequest request,
      List<FieldViolation> violations) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(URI.create(type));
    problem.setTitle(reasonPhrase(status));
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("traceId", RequestIdFilter.current(request));
    if (!violations.isEmpty()) {
      problem.setProperty("errors", violations);
    }
    return problem;
  }

  private String reasonPhrase(HttpStatusCode status) {
    return status instanceof HttpStatus httpStatus
        ? httpStatus.getReasonPhrase()
        : "Request failed";
  }
}
