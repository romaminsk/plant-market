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
        new RateLimitFilter(new ProblemResponseWriter(new ObjectMapper()), 2, 1, 10);

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
        new RateLimitFilter(new ProblemResponseWriter(new ObjectMapper()), 0, 0, 0);
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build());

    filter.filter(exchange, ignored -> Mono.empty()).block();

    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  void authEndpointsUseStricterLimit() {
    RateLimitFilter filter =
        new RateLimitFilter(new ProblemResponseWriter(new ObjectMapper()), 10, 1, 10);

    MockServerWebExchange first = authExchange();
    MockServerWebExchange second = authExchange();

    filter.filter(first, exchange -> Mono.empty()).block();
    filter.filter(second, exchange -> Mono.empty()).block();

    assertThat(first.getResponse().getStatusCode()).isNull();
    assertThat(second.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  @Test
  void oauthAuthorizeUsesPublicLimitNotTheLoginLimit() {
    RateLimitFilter filter =
        new RateLimitFilter(new ProblemResponseWriter(new ObjectMapper()), 2, 1, 10);

    MockServerWebExchange first = authorizeExchange();
    MockServerWebExchange second = authorizeExchange();
    MockServerWebExchange third = authorizeExchange();

    filter.filter(first, exchange -> Mono.empty()).block();
    filter.filter(second, exchange -> Mono.empty()).block();
    filter.filter(third, exchange -> Mono.empty()).block();

    assertThat(first.getResponse().getStatusCode()).isNull();
    assertThat(second.getResponse().getStatusCode()).isNull();
    assertThat(third.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  @Test
  void authenticatedPrincipalUsesTheHigherLimit() {
    RateLimitFilter filter =
        new RateLimitFilter(new ProblemResponseWriter(new ObjectMapper()), 1, 10, 10);

    MockServerWebExchange first = authenticatedExchange();
    MockServerWebExchange second = authenticatedExchange();

    filter.filter(first, exchange -> Mono.empty()).block();
    filter.filter(second, exchange -> Mono.empty()).block();

    assertThat(first.getResponse().getStatusCode()).isNull();
    assertThat(second.getResponse().getStatusCode()).isNull();
  }

  @Test
  void fakeBearerHeaderDoesNotGetTheAuthenticatedLimit() {
    RateLimitFilter filter =
        new RateLimitFilter(new ProblemResponseWriter(new ObjectMapper()), 1, 10, 10);

    MockServerWebExchange first = spoofedExchange();
    MockServerWebExchange second = spoofedExchange();

    filter.filter(first, exchange -> Mono.empty()).block();
    filter.filter(second, exchange -> Mono.empty()).block();

    assertThat(first.getResponse().getStatusCode()).isNull();
    assertThat(second.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  private MockServerWebExchange exchange() {
    return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products").build());
  }

  private MockServerWebExchange authorizeExchange() {
    return MockServerWebExchange.from(
        MockServerHttpRequest.get("/oauth2/authorize?client_id=storefront").build());
  }

  private MockServerWebExchange authExchange() {
    return MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/login").build());
  }

  private MockServerWebExchange authenticatedExchange() {
    return MockServerWebExchange.builder(MockServerHttpRequest.get("/api/v1/products").build())
        .principal((java.security.Principal) () -> "customer@example.test")
        .build();
  }

  private MockServerWebExchange spoofedExchange() {
    return MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/v1/products")
            .header("Authorization", "Bearer not-a-real-token")
            .build());
  }
}
