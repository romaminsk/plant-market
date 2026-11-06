package com.plantmarket.order.notification;

import com.plantmarket.order.domain.ClaimStatus;
import java.util.UUID;

public record ClaimDecidedEvent(
    UUID claimId, UUID orderId, String customerEmail, ClaimStatus status) {}
