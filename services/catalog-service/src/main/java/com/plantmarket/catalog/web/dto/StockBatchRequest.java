package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.Currency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record StockBatchRequest(
    @NotNull UUID productVariantId,
    @NotBlank @Size(max = 200) String supplier,
    @NotNull @Min(1) Integer quantity,
    @NotNull @PositiveOrZero Long unitCostMinor,
    @NotNull Currency currency,
    Instant receivedAt,
    @NotNull Instant quarantineUntil) {}
