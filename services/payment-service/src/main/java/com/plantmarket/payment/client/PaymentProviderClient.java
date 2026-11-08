package com.plantmarket.payment.client;

import java.util.UUID;

public interface PaymentProviderClient {

  ProviderPayment create(UUID orderId, long amountMinor, String currency);

  default ProviderPayment refund(String providerRef) {
    return new ProviderPayment(providerRef, "REFUNDED", null);
  }
}
