package com.plantmarket.gateway.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Converts the admin httpOnly access cookie into a bearer header before Spring Security runs.
 *
 * <p>Non-GET requests must carry {@code X-Admin-Request: 1}; browsers cannot add that header
 * cross-origin without a CORS preflight, which is an extra CSRF barrier on top of SameSite=Strict.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class AdminCookieAuthFilter implements WebFilter {

  static final String ACCESS_COOKIE = "pm.admin.access";
  static final String ADMIN_HEADER = "X-Admin-Request";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (authorization != null && !authorization.isBlank()) {
      return chain.filter(exchange);
    }
    var cookie = exchange.getRequest().getCookies().getFirst(ACCESS_COOKIE);
    if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
      return chain.filter(exchange);
    }
    String method = exchange.getRequest().getMethod().name();
    boolean safeMethod = "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    if (!safeMethod && !"1".equals(exchange.getRequest().getHeaders().getFirst(ADMIN_HEADER))) {
      return chain.filter(exchange);
    }
    ServerHttpRequest request =
        exchange
            .getRequest()
            .mutate()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + cookie.getValue())
            .build();
    return chain.filter(exchange.mutate().request(request).build());
  }
}
