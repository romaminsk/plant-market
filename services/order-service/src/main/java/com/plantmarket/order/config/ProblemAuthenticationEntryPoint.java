package com.plantmarket.order.config;

import com.plantmarket.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public ProblemAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    Map<String, Object> problem =
        Map.of(
            "type",
            "https://plant-market.by/problems/unauthorized",
            "title",
            "Unauthorized",
            "status",
            HttpServletResponse.SC_UNAUTHORIZED,
            "detail",
            "Authentication required",
            "instance",
            request.getRequestURI(),
            "traceId",
            RequestIdFilter.current(request));
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
