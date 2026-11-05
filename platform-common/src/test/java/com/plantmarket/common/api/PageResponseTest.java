package com.plantmarket.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageResponseTest {

  @Test
  void computesTotalPages() {
    PageResponse<String> page = PageResponse.of(List.of("a", "b"), 0, 2, 5);

    assertThat(page.totalPages()).isEqualTo(3);
    assertThat(page.items()).containsExactly("a", "b");
  }

  @Test
  void emptyPageHasZeroTotalPages() {
    PageResponse<String> page = PageResponse.of(List.of(), 0, 20, 0);

    assertThat(page.totalPages()).isZero();
    assertThat(page.totalElements()).isZero();
  }
}
