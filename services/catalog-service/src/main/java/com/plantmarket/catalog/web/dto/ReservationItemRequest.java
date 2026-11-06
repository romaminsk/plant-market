package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.ReservationTargetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReservationItemRequest(
    @NotNull ReservationTargetType targetType,
    @NotNull UUID targetId,
    @NotNull @Min(1) Integer quantity) {}
