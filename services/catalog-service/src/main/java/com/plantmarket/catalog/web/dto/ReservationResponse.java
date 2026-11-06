package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.ReservationStatus;
import com.plantmarket.catalog.domain.ReservationTargetType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
    UUID orderId, List<ReservationLine> reservations, Instant expiresAt) {

  public record ReservationLine(
      UUID id,
      ReservationTargetType targetType,
      UUID targetId,
      int quantity,
      ReservationStatus status) {}
}
