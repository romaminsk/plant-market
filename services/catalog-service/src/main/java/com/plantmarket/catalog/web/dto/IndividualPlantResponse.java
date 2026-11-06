package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.CareLevel;
import com.plantmarket.catalog.domain.Currency;
import com.plantmarket.catalog.domain.IndividualPlantStatus;
import com.plantmarket.catalog.domain.LightLevel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IndividualPlantResponse(
    UUID id,
    UUID productId,
    String productName,
    boolean variegated,
    String code,
    String description,
    String photoUrl,
    List<PlantPhotoResponse> photos,
    long priceMinor,
    Currency currency,
    IndividualPlantStatus status,
    CareLevel careLevel,
    LightLevel light,
    boolean petSafe,
    Instant createdAt,
    Instant updatedAt) {}
