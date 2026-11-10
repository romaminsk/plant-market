package com.plantmarket.order.web.dto;

import com.plantmarket.order.domain.DeliveryWindow;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CheckoutRequest(
    UUID shippingZoneId,
    Boolean thermalPack,
    @Size(max = 500) String deliveryAddress,
    LocalDate deliveryDate,
    DeliveryWindow deliveryWindow) {

  public CheckoutRequest(UUID shippingZoneId, Boolean thermalPack) {
    this(shippingZoneId, thermalPack, null, null, null);
  }

  public CheckoutRequest(UUID shippingZoneId, Boolean thermalPack, String deliveryAddress) {
    this(shippingZoneId, thermalPack, deliveryAddress, null, null);
  }
}
