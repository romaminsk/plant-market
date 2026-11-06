package com.plantmarket.order.client;

import java.util.UUID;

public interface PaymentClient {

  PaymentResult createPayment(UUID orderId, long amountMinor, String currency);
}
