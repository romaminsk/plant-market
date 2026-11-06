package com.plantmarket.order.web.dto;

import com.plantmarket.order.domain.ClaimStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClaimResponse(
    UUID id,
    UUID orderId,
    UUID customerId,
    ClaimStatus status,
    String reason,
    List<String> photoUrls,
    String decisionComment,
    Instant createdAt,
    Instant updatedAt,
    Instant decidedAt) {}
