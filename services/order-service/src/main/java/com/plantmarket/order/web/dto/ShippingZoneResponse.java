package com.plantmarket.order.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ShippingZoneResponse(
    UUID id,
    String name,
    String city,
    long priceMinor,
    long freeFromMinor,
    int minDays,
    int maxDays,
    boolean winterRequiresThermal,
    long thermalPackMinor,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}
