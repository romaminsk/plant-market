package com.plantmarket.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

  private final RequestIdFilter filter = new RequestIdFilter();

  @Test
  void keepsValidInboundRequestId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(RequestIdFilter.HEADER, "client-request-123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("client-request-123");
    assertThat(request.getAttribute(RequestIdFilter.ATTRIBUTE)).isEqualTo("client-request-123");
  }

  @Test
  void replacesInvalidRequestId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(RequestIdFilter.HEADER, "bad id with spaces");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    String requestId = response.getHeader(RequestIdFilter.HEADER);
    assertThat(requestId).isNotEqualTo("bad id with spaces");
    assertThat(requestId).hasSize(36);
  }
}
