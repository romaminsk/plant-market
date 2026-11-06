package com.plantmarket.order.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Public guest checkout: creates, reserves and pays an order without an account. */
public record GuestCheckoutRequest(
    @NotEmpty List<@Valid CreateOrderItemRequest> items,
    @NotBlank @Email @Size(max = 254) String guestEmail,
    @NotNull UUID shippingZoneId,
    Boolean thermalPack,
    @NotBlank @Size(max = 500) String deliveryAddress) {}
