package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.CareLevel;
import com.plantmarket.catalog.domain.Currency;
import com.plantmarket.catalog.domain.LightLevel;
import com.plantmarket.catalog.domain.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProductRequest(
    @NotBlank @Size(max = 64) String sku,
    @NotBlank @Size(max = 200) String name,
    @NotBlank
        @Size(max = 200)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "must be a lowercase slug")
        String slug,
    @Size(max = 5000) String description,
    @NotNull UUID categoryId,
    @NotNull @PositiveOrZero Long priceMinor,
    @NotNull Currency currency,
    @NotNull ProductStatus status,
    @NotNull CareLevel careLevel,
    @NotNull LightLevel light,
    @NotNull Boolean petSafe) {}
