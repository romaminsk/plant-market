package com.plantmarket.order.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ShippingZoneRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 120) String city,
    @NotNull @PositiveOrZero Long priceMinor,
    @NotNull @PositiveOrZero Long freeFromMinor,
    @NotNull @Min(0) Integer minDays,
    @NotNull @Min(0) Integer maxDays,
    @NotNull Boolean winterRequiresThermal,
    @NotNull @PositiveOrZero Long thermalPackMinor,
    Boolean active) {}
