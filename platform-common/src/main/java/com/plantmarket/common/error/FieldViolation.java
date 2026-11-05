package com.plantmarket.common.error;

/** One validation failure inside an RFC 7807 problem response. */
public record FieldViolation(String field, String message) {}
