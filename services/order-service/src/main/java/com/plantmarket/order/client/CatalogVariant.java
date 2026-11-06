package com.plantmarket.order.client;

import java.util.UUID;

public record CatalogVariant(
    UUID id, String sku, String name, long priceMinor, String currency, String status) {}
