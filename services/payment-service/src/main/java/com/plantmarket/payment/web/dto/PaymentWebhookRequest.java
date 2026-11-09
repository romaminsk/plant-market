package com.plantmarket.payment.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PaymentWebhookRequest(
    @NotBlank String providerRef,
    @NotBlank String status,
    @NotNull @PositiveOrZero Long amountMinor,
    @NotBlank @Size(max = 3) String currency) {}
