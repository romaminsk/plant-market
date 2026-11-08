package com.plantmarket.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionTest {

  @Test
  void factoriesCarryStatusAndProblemType() {
    assertThat(ApiException.badRequest("x").getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ApiException.unauthorized("x").getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(ApiException.forbidden("x").getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(ApiException.notFound("x").getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ApiException.conflict("x").getStatus()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(ApiException.conflict("x").getProblemType()).endsWith("conflict");
  }

  @Test
  void validationKeepsViolationsAndMessage() {
    List<FieldViolation> violations = List.of(new FieldViolation("email", "invalid"));
    ApiException exception = ApiException.validation("bad", violations);

    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getViolations()).isEqualTo(violations);
    assertThat(exception.getViolations().getFirst().field()).isEqualTo("email");
    assertThat(exception.getViolations().getFirst().message()).isEqualTo("invalid");
    assertThat(exception.getMessage()).isEqualTo("bad");
  }
}
