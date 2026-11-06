package com.plantmarket.order.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReservationResult(UUID orderId, List<Line> reservations, Instant expiresAt) {

  public record Line(UUID id, String targetType, UUID targetId, int quantity, String status) {}
}
