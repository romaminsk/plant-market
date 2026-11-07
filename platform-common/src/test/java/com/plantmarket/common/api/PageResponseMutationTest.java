package com.plantmarket.common.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.junit.Test;

/** JUnit 4 bridge for PIT. */
public class PageResponseMutationTest {

  @Test
  public void computesTotalPages() {
    PageResponse<String> page = PageResponse.of(List.of("a", "b"), 0, 2, 5);
    assertEquals(3, page.totalPages());
    assertEquals(2, page.items().size());
    assertEquals(5, page.totalElements());
  }

  @Test
  public void zeroSizeHasZeroPages() {
    PageResponse<String> page = PageResponse.of(List.of(), 0, 0, 0);
    assertEquals(0, page.totalPages());
  }

  @Test
  public void zeroSizeWithItemsStillHasZeroPages() {
    PageResponse<String> page = PageResponse.of(List.of("a"), 0, 0, 5);
    assertEquals(0, page.totalPages());
  }

  @Test
  public void copiesItems() {
    PageResponse<String> page = PageResponse.of(List.of("a"), 0, 10, 1);
    assertThrows(UnsupportedOperationException.class, () -> page.items().add("b"));
  }
}
