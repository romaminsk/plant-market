package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.IndividualPlantStatus;
import jakarta.validation.constraints.NotNull;

public record PlantTransitionRequest(@NotNull IndividualPlantStatus status) {}
