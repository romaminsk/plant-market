package com.plantmarket.e2e;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared access tokens for API E2E tests: the login rate limit is 10/min per IP and the seed runs
 * right before the suite, so logging in once per role keeps the suite under the limit.
 */
final class ApiTokens {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final HttpClient CLIENT = HttpClient.newHttpClient();
  private static String customerToken;
  private static long customerAt;
  private static String adminToken;
  private static long adminAt;
  private static String qaCustomerToken;
  private static long qaCustomerAt;

  private ApiTokens() {}

  static synchronized String customer(String apiUrl) throws Exception {
    if (customerToken == null || System.currentTimeMillis() - customerAt > 8 * 60_000L) {
      customerToken =
          login(apiUrl, "customer@plantmarket.local", "CustomerPassword123!", "customer");
      customerAt = System.currentTimeMillis();
    }
    return customerToken;
  }

  static synchronized String admin(String apiUrl) throws Exception {
    if (adminToken == null || System.currentTimeMillis() - adminAt > 8 * 60_000L) {
      adminToken = login(apiUrl, "admin@plantmarket.local", "AdminPassword123!", "admin");
      adminAt = System.currentTimeMillis();
    }
    return adminToken;
  }

  static String freshAdmin(String apiUrl) throws Exception {
    return login(apiUrl, "admin@plantmarket.local", "AdminPassword123!", "admin-fresh");
  }

  static String freshCustomer(String apiUrl) throws Exception {
    return login(apiUrl, "qa.customer@plantmarket.local", "QaCustomer123!", "qa-customer-fresh");
  }

  static synchronized String qaCustomer(String apiUrl) throws Exception {
    if (qaCustomerToken == null || System.currentTimeMillis() - qaCustomerAt > 8 * 60_000L) {
      qaCustomerToken =
          login(apiUrl, "qa.customer@plantmarket.local", "QaCustomer123!", "qa-customer");
      qaCustomerAt = System.currentTimeMillis();
    }
    return qaCustomerToken;
  }

  private static String login(String apiUrl, String email, String password, String role)
      throws Exception {
    String body = OBJECT_MAPPER.writeValueAsString(Map.of("email", email, "password", password));
    for (int attempt = 0; attempt < 12; attempt++) {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(apiUrl + "/api/v1/auth/login"))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        return OBJECT_MAPPER.readTree(response.body()).path("accessToken").asString();
      }
      if (response.statusCode() != 429) {
        throw new IllegalStateException(
            "Could not login " + role + ": " + response.statusCode() + " " + response.body());
      }
      Thread.sleep(5_000);
    }
    throw new IllegalStateException("Login for " + role + " stayed rate limited");
  }
}
