package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Cached seed lookups: keeps the E2E suite under the public rate limit. */
final class SeedData {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final HttpClient CLIENT = HttpClient.newHttpClient();
  private static final Map<String, String> PRODUCT_IDS = new HashMap<>();
  private static String variantId;

  private SeedData() {}

  static synchronized String productId(String apiUrl, String sku) throws Exception {
    String cached = PRODUCT_IDS.get(sku);
    if (cached != null) {
      return cached;
    }
    int page = 0;
    while (true) {
      JsonNode products = get(apiUrl, "/api/v1/products?page=" + page + "&size=100");
      for (JsonNode item : products.path("items")) {
        if (sku.equals(item.path("sku").asString())) {
          String id = item.path("id").asString();
          PRODUCT_IDS.put(sku, id);
          return id;
        }
      }
      if ((page + 1) * 100 >= products.path("totalElements").asInt()) {
        break;
      }
      page++;
    }
    throw new IllegalStateException("Seed product " + sku + " is missing");
  }

  static synchronized String variantId(String apiUrl) throws Exception {
    if (variantId != null) {
      return variantId;
    }
    String productId = productId(apiUrl, "MON-001");
    JsonNode variants = get(apiUrl, "/api/v1/products/" + productId + "/variants");
    variantId = variants.get(0).path("id").asString();
    return variantId;
  }

  private static JsonNode get(String apiUrl, String path) throws Exception {
    HttpResponse<String> response =
        CLIENT.send(
            HttpRequest.newBuilder(URI.create(apiUrl + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).as("response body: %s", response.body()).isBetween(200, 299);
    return OBJECT_MAPPER.readTree(response.body());
  }
}
