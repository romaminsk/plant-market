package com.plantmarket.order.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ClaimRequest(
    @NotBlank @Size(max = 2000) String reason,
    @NotEmpty @Size(max = 8) List<@NotBlank @Size(max = 1000) String> photoUrls) {}
