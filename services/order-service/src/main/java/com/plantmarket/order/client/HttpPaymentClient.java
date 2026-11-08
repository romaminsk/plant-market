package com.plantmarket.order.client;

import com.plantmarket.common.error.ApiException;
import com.plantmarket.order.security.ServiceTokenProvider;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpPaymentClient implements PaymentClient {

  private final RestClient restClient;

  public HttpPaymentClient(
      @Value("${app.payment.base-uri}") String baseUri, ServiceTokenProvider serviceTokens) {
    this.restClient =
        RestClient.builder()
            .baseUrl(baseUri)
            .requestInterceptor((request, body, execution) -> {
              request.getHeaders().setBearerAuth(serviceTokens.token());
              return execution.execute(request, body);
            })
            .build();
  }

  @Override
  public PaymentResult createPayment(UUID orderId, long amountMinor, String currency) {
    try {
      PaymentResult payment =
          restClient
              .post()
              .uri("/api/v1/payments")
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  Map.of(
                      "orderId", orderId.toString(),
                      "amountMinor", amountMinor,
                      "currency", currency))
              .retrieve()
              .body(PaymentResult.class);
      if (payment == null) {
        throw ApiException.conflict("Payment service returned an empty response");
      }
      return payment;
    } catch (RestClientResponseException exception) {
      throw ApiException.conflict(
          "Payment service rejected the payment: " + exception.getStatusCode());
    }
  }

  @Override
  public void refund(UUID paymentId) {
    restClient.post().uri("/api/v1/payments/{id}/refund", paymentId).retrieve().toBodilessEntity();
  }
}
