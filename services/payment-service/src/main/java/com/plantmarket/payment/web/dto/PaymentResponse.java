package com.plantmarket.payment.web.dto;

import com.plantmarket.payment.domain.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID orderId,
    long amountMinor,
    String currency,
    PaymentStatus status,
    String providerRef,
    String redirectUrl,
    Instant createdAt,
    Instant updatedAt) {}
