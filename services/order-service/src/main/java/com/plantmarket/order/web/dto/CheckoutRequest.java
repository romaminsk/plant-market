package com.plantmarket.order.web.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CheckoutRequest(
    UUID shippingZoneId, Boolean thermalPack, @Size(max = 500) String deliveryAddress) {

  public CheckoutRequest(UUID shippingZoneId, Boolean thermalPack) {
    this(shippingZoneId, thermalPack, null);
  }
}
