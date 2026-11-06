package com.plantmarket.order.web.dto;

import com.plantmarket.order.domain.OrderItemType;
import java.util.UUID;

public record OrderItemResponse(
    UUID id,
    OrderItemType itemType,
    UUID targetId,
    String name,
    long unitPriceMinor,
    int quantity,
    long lineTotalMinor) {}
