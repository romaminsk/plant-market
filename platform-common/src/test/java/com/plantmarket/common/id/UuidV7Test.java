package com.plantmarket.common.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

  @Test
  void generatesVersion7WithRfcVariant() {
    UUID id = UuidV7.generate();

    assertThat(id.version()).isEqualTo(7);
    assertThat(id.variant()).isEqualTo(2);
  }

  @Test
  void generatesUniqueIdentifiers() {
    Set<UUID> ids = new HashSet<>();
    for (int i = 0; i < 10_000; i++) {
      assertThat(ids.add(UuidV7.generate())).isTrue();
    }
  }
}
