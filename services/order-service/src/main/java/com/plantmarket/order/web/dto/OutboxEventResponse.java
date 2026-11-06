package com.plantmarket.order.web.dto;

import java.time.Instant;
import java.util.UUID;

public record OutboxEventResponse(
    UUID id,
    UUID aggregateId,
    String eventType,
    int attempts,
    String lastError,
    boolean dead,
    Instant createdAt,
    Instant publishedAt,
    Instant nextAttemptAt) {}
