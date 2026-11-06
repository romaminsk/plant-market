package com.plantmarket.catalog.web.dto;

import com.plantmarket.catalog.domain.PlantPhotoKind;

public record PlantPhotoResponse(String url, PlantPhotoKind kind) {}
