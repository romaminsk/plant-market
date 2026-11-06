package com.plantmarket.order.client;

import java.util.UUID;

public record PaymentResult(UUID id, String status) {}
