package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.PlantPhotoKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlantPhotoRequest(
    @NotBlank @Size(max = 1000) String url, @NotNull PlantPhotoKind kind) {}
