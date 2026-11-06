package com.plantmarket.order.client;

import java.util.List;
import java.util.UUID;

public record ReservationRequest(UUID orderId, List<Item> items) {

  public record Item(ReservationTargetType targetType, UUID targetId, int quantity) {}
}
