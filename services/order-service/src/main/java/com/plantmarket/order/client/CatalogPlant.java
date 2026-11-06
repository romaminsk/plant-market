package com.plantmarket.order.client;

import java.util.UUID;

public record CatalogPlant(
    UUID id, String productName, String code, long priceMinor, String currency, String status) {}
