package com.plantmarket.catalog.web.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
    UUID id, String name, String slug, String description, Instant createdAt, Instant updatedAt) {}
