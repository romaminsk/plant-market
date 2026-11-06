package com.plantmarket.order.client;

import com.plantmarket.common.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpCatalogClient implements CatalogClient {

  private final RestClient restClient;

  public HttpCatalogClient(@Value("${app.catalog.base-uri}") String baseUri) {
    this.restClient = RestClient.builder().baseUrl(baseUri).build();
  }

  @Override
  public CatalogVariant getVariant(UUID id) {
    try {
      CatalogVariant variant =
          restClient.get().uri("/api/v1/variants/{id}", id).retrieve().body(CatalogVariant.class);
      if (variant == null) {
        throw ApiException.badRequest("Variant not found: " + id);
      }
      return variant;
    } catch (RestClientResponseException exception) {
      throw ApiException.badRequest("Variant not found: " + id);
    }
  }

  @Override
  public CatalogPlant getPlant(UUID id) {
    try {
      CatalogPlant plant =
          restClient
              .get()
              .uri("/api/v1/individual-plants/{id}", id)
              .retrieve()
              .body(CatalogPlant.class);
      if (plant == null) {
        throw ApiException.badRequest("Individual plant not found: " + id);
      }
      return plant;
    } catch (RestClientResponseException exception) {
      throw ApiException.badRequest("Individual plant not found: " + id);
    }
  }

  @Override
  public ReservationResult reserve(UUID orderId, List<ReservationRequest.Item> items) {
    try {
      ReservationResult result =
          restClient
              .post()
              .uri("/api/v1/reservations")
              .contentType(MediaType.APPLICATION_JSON)
              .body(new ReservationRequest(orderId, items))
              .retrieve()
              .body(ReservationResult.class);
      if (result == null) {
        throw ApiException.conflict("Catalog reservation failed");
      }
      return result;
    } catch (RestClientResponseException exception) {
      throw ApiException.conflict("Catalog reservation failed: " + exception.getStatusCode());
    }
  }

  @Override
  public void release(UUID orderId) {
    restClient
        .post()
        .uri("/api/v1/reservations/{orderId}/release", orderId)
        .retrieve()
        .toBodilessEntity();
  }

  @Override
  public void confirm(UUID orderId) {
    restClient
        .post()
        .uri("/api/v1/reservations/{orderId}/confirm", orderId)
        .retrieve()
        .toBodilessEntity();
  }
}
