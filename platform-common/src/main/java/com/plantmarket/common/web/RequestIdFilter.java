package com.plantmarket.common.web;

import com.plantmarket.common.id.UuidV7;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Accepts an inbound request id or creates one, exposes it to logs and echoes it in the response.
 *
 * <p>Runs before Spring Security so even 401/403 responses carry a correlation id.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Request-Id";
  public static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";
  public static final String MDC_KEY = "requestId";

  private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{8,128}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = request.getHeader(HEADER);
    if (requestId == null || !VALID_REQUEST_ID.matcher(requestId).matches()) {
      requestId = UuidV7.generate().toString();
    }

    request.setAttribute(ATTRIBUTE, requestId);
    response.setHeader(HEADER, requestId);
    MDC.put(MDC_KEY, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  public static String current(HttpServletRequest request) {
    Object requestId = request.getAttribute(ATTRIBUTE);
    if (requestId instanceof String value && !value.isBlank()) {
      return value;
    }
    String header = request.getHeader(HEADER);
    return header == null || header.isBlank() ? UuidV7.generate().toString() : header;
  }
}
