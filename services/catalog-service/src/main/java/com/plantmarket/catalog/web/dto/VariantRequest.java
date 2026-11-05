package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.Currency;
import com.plantmarket.catalog.domain.VariantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record VariantRequest(
    @NotBlank @Size(max = 64) String sku,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 32) String size,
    @Size(max = 32) String potSize,
    @Size(max = 32) String form,
    @NotNull @PositiveOrZero Long priceMinor,
    @NotNull Currency currency,
    @NotNull VariantStatus status) {}
