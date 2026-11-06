package com.plantmarket.order.web.dto;

import com.plantmarket.order.domain.ClaimDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClaimDecisionRequest(
    @NotNull ClaimDecision decision, @Size(max = 2000) String comment) {}
