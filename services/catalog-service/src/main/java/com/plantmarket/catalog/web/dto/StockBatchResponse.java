package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.Currency;
import com.plantmarket.catalog.domain.StockBatchStatus;
import java.time.Instant;
import java.util.UUID;

public record StockBatchResponse(
    UUID id,
    UUID productVariantId,
    String supplier,
    int quantity,
    int remainingQuantity,
    long unitCostMinor,
    Currency currency,
    Instant receivedAt,
    Instant quarantineUntil,
    StockBatchStatus status,
    Instant createdAt,
    Instant updatedAt) {}
