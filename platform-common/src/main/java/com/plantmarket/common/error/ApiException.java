package com.plantmarket.common.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Application exception carrying the HTTP status and RFC 7807 problem type. */
public class ApiException extends RuntimeException {

  public static final String PROBLEM_BASE = "https://plant-market.by/problems/";

  private final HttpStatus status;
  private final String problemType;
  private final List<FieldViolation> violations;

  public ApiException(HttpStatus status, String problemType, String detail) {
    this(status, problemType, detail, List.of());
  }

  public ApiException(
      HttpStatus status, String problemType, String detail, List<FieldViolation> violations) {
    super(detail);
    this.status = status;
    this.problemType = problemType;
    this.violations = List.copyOf(violations);
  }

  public static ApiException badRequest(String detail) {
    return new ApiException(HttpStatus.BAD_REQUEST, PROBLEM_BASE + "bad-request", detail);
  }

  public static ApiException validation(String detail, List<FieldViolation> violations) {
    return new ApiException(
        HttpStatus.BAD_REQUEST, PROBLEM_BASE + "validation", detail, violations);
  }

  public static ApiException unauthorized(String detail) {
    return new ApiException(HttpStatus.UNAUTHORIZED, PROBLEM_BASE + "unauthorized", detail);
  }

  public static ApiException forbidden(String detail) {
    return new ApiException(HttpStatus.FORBIDDEN, PROBLEM_BASE + "forbidden", detail);
  }

  public static ApiException notFound(String detail) {
    return new ApiException(HttpStatus.NOT_FOUND, PROBLEM_BASE + "not-found", detail);
  }

  public static ApiException conflict(String detail) {
    return new ApiException(HttpStatus.CONFLICT, PROBLEM_BASE + "conflict", detail);
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getProblemType() {
    return problemType;
  }

  public List<FieldViolation> getViolations() {
    return violations;
  }
}
