package com.plantmarket.order.web.dto;

import java.time.Instant;
import java.util.UUID;

public record CareReminderResponse(
    UUID id,
    UUID orderId,
    UUID orderItemId,
    String plantName,
    int intervalDays,
    Instant nextSendAt,
    boolean active,
    Instant createdAt) {}
