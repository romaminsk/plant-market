package com.plantmarket.common.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/** JUnit 4 bridge for PIT. */
public class ApiExceptionMutationTest {

  @Test
  public void factoriesCarryStatusAndProblemType() {
    assertEquals(HttpStatus.BAD_REQUEST, ApiException.badRequest("x").getStatus());
    assertEquals(HttpStatus.UNAUTHORIZED, ApiException.unauthorized("x").getStatus());
    assertEquals(HttpStatus.FORBIDDEN, ApiException.forbidden("x").getStatus());
    assertEquals(HttpStatus.NOT_FOUND, ApiException.notFound("x").getStatus());
    assertEquals(HttpStatus.CONFLICT, ApiException.conflict("x").getStatus());
    assertTrue(ApiException.conflict("x").getProblemType().endsWith("conflict"));
  }

  @Test
  public void validationKeepsViolations() {
    List<FieldViolation> violations = List.of(new FieldViolation("email", "invalid"));
    ApiException exception = ApiException.validation("bad", violations);
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    assertEquals(violations, exception.getViolations());
    assertEquals("bad", exception.getMessage());
  }
}
