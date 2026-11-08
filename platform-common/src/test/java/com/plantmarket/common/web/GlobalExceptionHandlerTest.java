package com.plantmarket.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.plantmarket.common.error.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MissingServletRequestParameterException;

class GlobalExceptionHandlerTest {

  @Test
  void mapsOptimisticLockToConflict() {
    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/variants/1");
    request.addHeader("X-Request-Id", "test-request-123");

    ResponseEntity<ProblemDetail> response =
        new GlobalExceptionHandler()
            .handleOptimisticLock(
                new ObjectOptimisticLockingFailureException("ProductVariantEntity", "1"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getProperties()).containsEntry("traceId", "test-request-123");
    assertThat(response.getBody().getDetail()).contains("modified");
  }

  @Test
  void mapsApiExceptionToProblem() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products/1");
    request.addHeader("X-Request-Id", "test-request-456");

    ResponseEntity<ProblemDetail> response =
        new GlobalExceptionHandler().handleApiException(ApiException.conflict("nope"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetail()).isEqualTo("nope");
    assertThat(response.getBody().getProperties()).containsEntry("traceId", "test-request-456");
  }

  @Test
  void mapsMalformedRequestToBadRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");

    ResponseEntity<ProblemDetail> response =
        new GlobalExceptionHandler()
            .handleMalformedRequest(
                new MissingServletRequestParameterException("size", "int"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
  }

  @Test
  void mapsUnexpectedToInternalServerError() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");

    ResponseEntity<ProblemDetail> response =
        new GlobalExceptionHandler().handleUnexpected(new RuntimeException("boom"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
  }

  @Test
  void mapsErrorResponseStatus() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products/1");

    ResponseEntity<ProblemDetail> response =
        new GlobalExceptionHandler()
            .handleErrorResponse(new ErrorResponseException(HttpStatus.NOT_FOUND), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
  }
}
