package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.CareLevel;
import com.plantmarket.catalog.domain.Currency;
import com.plantmarket.catalog.domain.LightLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record IndividualPlantRequest(
    @NotBlank @Size(max = 64) String code,
    @Size(max = 5000) String description,
    @Size(max = 1000) String photoUrl,
    @NotNull @PositiveOrZero Long priceMinor,
    @NotNull Currency currency,
    @NotNull CareLevel careLevel,
    @NotNull LightLevel light,
    @NotNull Boolean petSafe) {}
