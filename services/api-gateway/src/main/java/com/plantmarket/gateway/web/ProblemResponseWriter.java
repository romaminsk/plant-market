package com.plantmarket.gateway.web;

import com.plantmarket.common.error.ApiException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/** Writes RFC 7807 responses from WebFlux filters that run outside @RestControllerAdvice. */
@Component
public class ProblemResponseWriter {

  private final ObjectMapper objectMapper;

  public ProblemResponseWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Mono<Void> write(
      ServerWebExchange exchange, HttpStatus status, String problemType, String detail) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", ApiException.PROBLEM_BASE + problemType);
    body.put("title", status.getReasonPhrase());
    body.put("status", status.value());
    body.put("detail", detail);
    body.put("instance", exchange.getRequest().getPath().value());
    body.put("traceId", RequestIdWebFilter.current(exchange));

    byte[] bytes;
    try {
      bytes = objectMapper.writeValueAsBytes(body);
    } catch (Exception exception) {
      return Mono.error(exception);
    }
    DataBuffer buffer = response.bufferFactory().wrap(bytes);
    return response.writeWith(Mono.just(buffer));
  }
}
