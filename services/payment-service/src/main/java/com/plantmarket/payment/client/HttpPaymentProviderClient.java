package com.plantmarket.payment.client;

import com.plantmarket.common.error.ApiException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpPaymentProviderClient implements PaymentProviderClient {

  private final RestClient restClient;

  public HttpPaymentProviderClient(
      @Value("${app.payment.provider-uri}") String providerUri,
      @Value("${app.http.connect-timeout:PT2S}") Duration connectTimeout,
      @Value("${app.http.read-timeout:PT5S}") Duration readTimeout) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
    requestFactory.setReadTimeout((int) readTimeout.toMillis());
    this.restClient =
        RestClient.builder().baseUrl(providerUri).requestFactory(requestFactory).build();
  }

  @Override
  public ProviderPayment refund(String providerRef) {
    try {
      ProviderPayment payment =
          restClient
              .post()
              .uri("/provider/refunds")
              .contentType(MediaType.APPLICATION_JSON)
              .body(Map.of("providerRef", providerRef))
              .retrieve()
              .body(ProviderPayment.class);
      if (payment == null) {
        throw ApiException.conflict("Payment provider returned an empty refund response");
      }
      return payment;
    } catch (RestClientResponseException exception) {
      throw ApiException.conflict(
          "Payment provider rejected the refund: " + exception.getStatusCode());
    }
  }

  @Override
  public ProviderPayment create(UUID orderId, long amountMinor, String currency) {
    try {
      ProviderPayment payment =
          restClient
              .post()
              .uri("/provider/payments")
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  Map.of(
                      "orderId", orderId.toString(),
                      "amountMinor", amountMinor,
                      "currency", currency))
              .retrieve()
              .body(ProviderPayment.class);
      if (payment == null) {
        throw ApiException.conflict("Payment provider returned an empty response");
      }
      return payment;
    } catch (RestClientResponseException exception) {
      throw ApiException.conflict(
          "Payment provider rejected the payment: " + exception.getStatusCode());
    }
  }
}
