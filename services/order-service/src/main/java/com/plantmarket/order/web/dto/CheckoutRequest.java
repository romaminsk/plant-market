package com.plantmarket.order.web.dto;

import java.util.UUID;

public record CheckoutRequest(UUID shippingZoneId, Boolean thermalPack) {}
