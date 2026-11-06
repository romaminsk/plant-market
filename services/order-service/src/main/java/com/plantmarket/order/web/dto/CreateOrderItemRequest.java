package com.plantmarket.order.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOrderItemRequest(
    UUID variantId, UUID individualPlantId, @NotNull @Min(1) Integer quantity) {}
