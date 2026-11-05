package com.plantmarket.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plantmarket.common.error.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilter(new RequestIdFilter(), "/*")
            .build();
  }

  @Test
  void mapsApiExceptionToProblemDetail() throws Exception {
    mockMvc
        .perform(get("/api/test/not-found").header(RequestIdFilter.HEADER, "test-request-123"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(header().string(RequestIdFilter.HEADER, "test-request-123"))
        .andExpect(jsonPath("$.type").value(ApiException.PROBLEM_BASE + "not-found"))
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Missing"))
        .andExpect(jsonPath("$.instance").value("/api/test/not-found"))
        .andExpect(jsonPath("$.traceId").value("test-request-123"));
  }

  @Test
  void mapsValidationErrors() throws Exception {
    mockMvc
        .perform(
            post("/api/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value(ApiException.PROBLEM_BASE + "validation"))
        .andExpect(jsonPath("$.errors[0].field").value("name"));
  }

  @Test
  void mapsConflictAndUnexpectedErrors() throws Exception {
    mockMvc
        .perform(get("/api/test/conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type").value(ApiException.PROBLEM_BASE + "conflict"));

    mockMvc
        .perform(get("/api/test/error"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.type").value(ApiException.PROBLEM_BASE + "internal"))
        .andExpect(jsonPath("$.detail").value("Unexpected server error"));
  }

  @RestController
  static class TestController {

    @GetMapping("/api/test/not-found")
    void notFound() {
      throw ApiException.notFound("Missing");
    }

    @GetMapping("/api/test/conflict")
    void conflict() {
      throw ApiException.conflict("Duplicate");
    }

    @GetMapping("/api/test/error")
    void error() {
      throw new IllegalStateException("boom");
    }

    @PostMapping("/api/test/validation")
    void validation(@Valid @RequestBody Payload payload) {}
  }

  record Payload(@NotBlank String name) {}
}
