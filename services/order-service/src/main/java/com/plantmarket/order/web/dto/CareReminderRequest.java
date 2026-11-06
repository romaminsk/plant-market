package com.plantmarket.order.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CareReminderRequest(@NotNull @Min(1) @Max(365) Integer intervalDays) {}
