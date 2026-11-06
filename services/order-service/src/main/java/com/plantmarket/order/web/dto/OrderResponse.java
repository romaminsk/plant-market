package com.plantmarket.order.web.dto;

import com.plantmarket.order.domain.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID customerId,
    String customerEmail,
    UUID shippingZoneId,
    boolean thermalPack,
    OrderStatus status,
    long subtotalMinor,
    long shippingMinor,
    long taxMinor,
    long discountMinor,
    long totalMinor,
    String currency,
    Instant reservationExpiresAt,
    List<OrderItemResponse> items,
    Instant createdAt,
    Instant updatedAt) {}
