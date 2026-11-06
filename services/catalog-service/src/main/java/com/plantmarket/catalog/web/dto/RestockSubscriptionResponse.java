package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.RestockSubscriptionStatus;
import com.plantmarket.catalog.domain.RestockTargetType;
import java.time.Instant;
import java.util.UUID;

public record RestockSubscriptionResponse(
    UUID id,
    RestockTargetType targetType,
    UUID targetId,
    RestockSubscriptionStatus status,
    Instant createdAt,
    Instant notifiedAt) {}
