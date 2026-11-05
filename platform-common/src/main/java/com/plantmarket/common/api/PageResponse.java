package com.plantmarket.common.api;

import java.util.List;

/** Stable pagination envelope for REST responses. */
public record PageResponse<T>(
    List<T> items, int page, int size, long totalElements, int totalPages) {

  public PageResponse {
    items = List.copyOf(items);
  }

  public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalElements) {
    int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    return new PageResponse<>(items, page, size, totalElements, totalPages);
  }
}
