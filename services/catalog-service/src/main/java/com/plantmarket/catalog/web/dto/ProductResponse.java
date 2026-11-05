package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.CareLevel;
import com.plantmarket.catalog.domain.Currency;
import com.plantmarket.catalog.domain.LightLevel;
import com.plantmarket.catalog.domain.ProductStatus;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String sku,
    String name,
    String slug,
    String description,
    UUID categoryId,
    long priceMinor,
    Currency currency,
    ProductStatus status,
    CareLevel careLevel,
    LightLevel light,
    boolean petSafe,
    Instant createdAt,
    Instant updatedAt) {}
