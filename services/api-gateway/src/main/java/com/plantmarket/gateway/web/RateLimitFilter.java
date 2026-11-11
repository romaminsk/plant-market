package com.plantmarket.gateway.web;

import java.net.InetSocketAddress;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Fixed-window limiter. The store is Redis-backed in compose, so several gateway instances share
 * one window; local runs without Redis fall back to an in-memory store.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

  private static final Duration WINDOW = Duration.ofMinutes(1);

  private final ProblemResponseWriter responseWriter;
  private final int publicRequestsPerMinute;
  private final int authRequestsPerMinute;
  private final int authenticatedRequestsPerMinute;
  private final boolean trustForwarded;
  private final RateLimitStore store;

  public RateLimitFilter(
      ProblemResponseWriter responseWriter,
      int publicRequestsPerMinute,
      int authRequestsPerMinute,
      int authenticatedRequestsPerMinute) {
    this(
        responseWriter,
        publicRequestsPerMinute,
        authRequestsPerMinute,
        authenticatedRequestsPerMinute,
        false,
        new InMemoryRateLimitStore());
  }

  public RateLimitFilter(
      ProblemResponseWriter responseWriter,
      int publicRequestsPerMinute,
      int authRequestsPerMinute,
      int authenticatedRequestsPerMinute,
      boolean trustForwarded) {
    this(
        responseWriter,
        publicRequestsPerMinute,
        authRequestsPerMinute,
        authenticatedRequestsPerMinute,
        trustForwarded,
        new InMemoryRateLimitStore());
  }

  @Autowired
  public RateLimitFilter(
      ProblemResponseWriter responseWriter,
      @Value("${app.rate-limit.public-rpm:60}") int publicRequestsPerMinute,
      @Value("${app.rate-limit.auth-rpm:10}") int authRequestsPerMinute,
      @Value("${app.rate-limit.authenticated-rpm:300}") int authenticatedRequestsPerMinute,
      @Value("${app.rate-limit.trust-forwarded:false}") boolean trustForwarded,
      RateLimitStore store) {
    this.responseWriter = responseWriter;
    this.publicRequestsPerMinute = publicRequestsPerMinute;
    this.authRequestsPerMinute = authRequestsPerMinute;
    this.authenticatedRequestsPerMinute = authenticatedRequestsPerMinute;
    this.trustForwarded = trustForwarded;
    this.store = store;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().value();
    if (!path.startsWith("/api/") && !path.equals("/login") && !path.startsWith("/oauth2/")) {
      return chain.filter(exchange);
    }

    return exchange
        .getPrincipal()
        .map(principal -> !"anonymousUser".equals(principal.getName()))
        .defaultIfEmpty(false)
        .flatMap(authenticated -> limit(exchange, chain, path, authenticated));
  }

  private Mono<Void> limit(
      ServerWebExchange exchange, GatewayFilterChain chain, String path, boolean authenticated) {
    String key = clientKey(exchange) + ":" + (authenticated ? "auth:" : "public:") + routeKey(path);
    int limit = limitFor(path, authenticated);
    return store
        .increment(key, WINDOW)
        .flatMap(window -> {
          if (window.count() > limit) {
            long retryAfterSeconds = Math.max(1, window.resetAfterMillis() / 1000);
            exchange
                .getResponse()
                .getHeaders()
                .set("Retry-After", Long.toString(retryAfterSeconds));
            return responseWriter.write(
                exchange, HttpStatus.TOO_MANY_REQUESTS, "rate-limit", "Rate limit exceeded");
          }
          return chain.filter(exchange);
        });
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }

  private int limitFor(String path, boolean authenticated) {
    if (path.equals("/api/v1/auth/login")
        || path.equals("/api/v1/auth/register")
        || path.equals("/login")
        || path.equals("/oauth2/token")) {
      return authRequestsPerMinute;
    }
    return authenticated ? authenticatedRequestsPerMinute : publicRequestsPerMinute;
  }

  private String clientKey(ServerWebExchange exchange) {
    if (trustForwarded) {
      String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
      if (forwarded != null && !forwarded.isBlank()) {
        return forwarded.split(",")[0].trim();
      }
    }
    InetSocketAddress address = exchange.getRequest().getRemoteAddress();
    return address == null || address.getAddress() == null
        ? "unknown"
        : address.getAddress().getHostAddress();
  }

  private String routeKey(String path) {
    String[] segments = path.split("/");
    return segments.length > 3 ? segments[3] : "root";
  }
}
