package com.plantmarket.catalog.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AdjustStockRequest(@NotNull @PositiveOrZero Integer remainingQuantity) {}
