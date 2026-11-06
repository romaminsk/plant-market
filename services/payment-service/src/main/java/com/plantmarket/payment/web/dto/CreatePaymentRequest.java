package com.plantmarket.payment.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CreatePaymentRequest(
    @NotNull UUID orderId,
    @NotNull @Positive Long amountMinor,
    @NotBlank @Pattern(regexp = "BYN") String currency) {}
