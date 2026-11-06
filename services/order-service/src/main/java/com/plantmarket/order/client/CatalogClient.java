package com.plantmarket.order.client;

import java.util.List;
import java.util.UUID;

public interface CatalogClient {

  CatalogVariant getVariant(UUID id);

  CatalogPlant getPlant(UUID id);

  ReservationResult reserve(UUID orderId, List<ReservationRequest.Item> items);

  void release(UUID orderId);

  void confirm(UUID orderId);
}
