package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.Currency;
import com.plantmarket.catalog.domain.VariantStatus;
import java.time.Instant;
import java.util.UUID;

public record VariantResponse(
    UUID id,
    UUID productId,
    String sku,
    String name,
    String size,
    String potSize,
    String form,
    long priceMinor,
    Currency currency,
    VariantStatus status,
    int availableQuantity,
    Instant createdAt,
    Instant updatedAt) {}
