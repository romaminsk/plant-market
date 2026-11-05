package com.plantmarket.gateway.web;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Phase 0 fixed-window limiter. Deliberately in-memory: Redis is out of scope until A14 triggers.
 * Replace with a distributed limiter before running more than one gateway instance.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

  private static final long WINDOW_MILLIS = 60_000;
  private static final int MAX_TRACKED_KEYS = 10_000;

  private final ProblemResponseWriter responseWriter;
  private final int publicRequestsPerMinute;
  private final int authRequestsPerMinute;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  public RateLimitFilter(
      ProblemResponseWriter responseWriter,
      @Value("${app.rate-limit.public-rpm:60}") int publicRequestsPerMinute,
      @Value("${app.rate-limit.auth-rpm:10}") int authRequestsPerMinute) {
    this.responseWriter = responseWriter;
    this.publicRequestsPerMinute = publicRequestsPerMinute;
    this.authRequestsPerMinute = authRequestsPerMinute;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().value();
    if (!path.startsWith("/api/")) {
      return chain.filter(exchange);
    }

    long now = System.currentTimeMillis();
    String key = clientKey(exchange) + ":" + routeKey(path);
    Window window =
        windows.compute(
            key,
            (ignored, existing) ->
                existing == null || now - existing.startMillis >= WINDOW_MILLIS
                    ? new Window(now)
                    : existing);

    if (window.count.incrementAndGet() > limitFor(path)) {
      long retryAfterSeconds = Math.max(1, (WINDOW_MILLIS - (now - window.startMillis)) / 1000);
      exchange.getResponse().getHeaders().set("Retry-After", Long.toString(retryAfterSeconds));
      return responseWriter.write(
          exchange, HttpStatus.TOO_MANY_REQUESTS, "rate-limit", "Rate limit exceeded");
    }

    if (windows.size() > MAX_TRACKED_KEYS) {
      windows.entrySet().removeIf(entry -> now - entry.getValue().startMillis >= WINDOW_MILLIS);
    }
    return chain.filter(exchange);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }

  private int limitFor(String path) {
    return path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/register")
        ? authRequestsPerMinute
        : publicRequestsPerMinute;
  }

  private String clientKey(ServerWebExchange exchange) {
    InetSocketAddress address = exchange.getRequest().getRemoteAddress();
    return address == null || address.getAddress() == null
        ? "unknown"
        : address.getAddress().getHostAddress();
  }

  private String routeKey(String path) {
    String[] segments = path.split("/");
    return segments.length > 3 ? segments[3] : "root";
  }

  private static final class Window {
    private final long startMillis;
    private final AtomicInteger count = new AtomicInteger();

    private Window(long startMillis) {
      this.startMillis = startMillis;
    }
  }
}
