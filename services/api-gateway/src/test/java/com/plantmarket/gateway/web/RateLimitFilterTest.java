package com.plantmarket.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

class RateLimitFilterTest {

  @Test
  void rejectsRequestsAboveTheWindowLimit() {
    RateLimitFilter filter =
        new RateLimitFilter(new ProblemResponseWriter(new ObjectMapper()), 2, 1);

    MockServerWebExchange first = exchange();
    MockServerWebExchange second = exchange();
    MockServerWebExchange third = exchange();

    filter.filter(first, exchange -> Mono.empty()).block();
    filter.filter(second, exchange -> Mono.empty()).block();
    filter.filter(third, exchange -> Mono.empty()).block();

    assertThat(first.getResponse().getStatusCode()).isNull();
    assertThat(second.getResponse().getStatusCode()).isNull();
    assertThat(third.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(third.getResponse().getHeaders().getFirst("Retry-After")).isNotBlank();
    assertThat(third.getResponse().getBodyAsString().block())
        .contains("\"status\":429")
        .contains("rate-limit");
  }

  @Test
  void ignoresNonApiPaths() {
    RateLimitFilter filter =
        new RateLimitFilter(new ProblemResponseWriter(new ObjectMapper()), 0, 0);
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build());

    filter.filter(exchange, ignored -> Mono.empty()).block();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  void authEndpointsUseStricterLimit() {
    RateLimitFilter filter =
        new RateLimitFilter(new ProblemResponseWriter(new ObjectMapper()), 10, 1);

    MockServerWebExchange first = authExchange();
    MockServerWebExchange second = authExchange();

    filter.filter(first, exchange -> Mono.empty()).block();
    filter.filter(second, exchange -> Mono.empty()).block();

    assertThat(first.getResponse().getStatusCode()).isNull();
    assertThat(second.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  private MockServerWebExchange exchange() {
    return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products").build());
  }

  private MockServerWebExchange authExchange() {
    return MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/login").build());
  }
}
