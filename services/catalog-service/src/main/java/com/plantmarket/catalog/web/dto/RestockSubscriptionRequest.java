package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.RestockTargetType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RestockSubscriptionRequest(
    @NotNull RestockTargetType targetType,
    @NotNull UUID targetId,
    @NotBlank @Email @Size(max = 254) String email) {}
