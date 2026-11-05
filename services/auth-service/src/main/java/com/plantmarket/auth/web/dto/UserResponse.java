package com.plantmarket.auth.web.dto;

import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String email, String fullName, List<String> roles) {}
