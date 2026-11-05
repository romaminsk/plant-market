package com.plantmarket.gateway.web;

import com.plantmarket.common.id.UuidV7;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Creates or forwards a correlation id before security and routing run. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdWebFilter implements WebFilter {

  public static final String HEADER = "X-Request-Id";
  public static final String ATTRIBUTE = RequestIdWebFilter.class.getName() + ".requestId";

  private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{8,128}");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String requestId = exchange.getRequest().getHeaders().getFirst(HEADER);
    if (requestId == null || !VALID_REQUEST_ID.matcher(requestId).matches()) {
      requestId = UuidV7.generate().toString();
    }
    String resolvedRequestId = requestId;

    ServerHttpRequest request =
        exchange
            .getRequest()
            .mutate()
            .headers(headers -> headers.set(HEADER, resolvedRequestId))
            .build();
    exchange.getAttributes().put(ATTRIBUTE, resolvedRequestId);
    exchange.getResponse().getHeaders().set(HEADER, resolvedRequestId);
    return chain.filter(exchange.mutate().request(request).build());
  }

  public static String current(ServerWebExchange exchange) {
    Object requestId = exchange.getAttribute(ATTRIBUTE);
    if (requestId instanceof String value && !value.isBlank()) {
      return value;
    }
    String header = exchange.getRequest().getHeaders().getFirst(HEADER);
    return header == null || header.isBlank() ? UuidV7.generate().toString() : header;
  }
}
