package com.plantmarket.payment.web.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentWebhookRequest(@NotBlank String providerRef, @NotBlank String status) {}
