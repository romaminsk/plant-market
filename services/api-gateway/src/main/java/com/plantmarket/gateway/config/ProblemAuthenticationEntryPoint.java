package com.plantmarket.gateway.config;

import com.plantmarket.gateway.web.ProblemResponseWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ProblemAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

  private final ProblemResponseWriter responseWriter;

  public ProblemAuthenticationEntryPoint(ProblemResponseWriter responseWriter) {
    this.responseWriter = responseWriter;
  }

  @Override
  public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
    return responseWriter.write(
        exchange, HttpStatus.UNAUTHORIZED, "unauthorized", "Authentication required");
  }
}
