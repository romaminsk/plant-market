package com.plantmarket.common.id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;

/** JUnit 4 bridge for PIT: the main suite uses JUnit 6, PIT's JUnit 5 plugin does not run it. */
public class UuidV7MutationTest {

  @Test
  public void generatesVersion7WithRfcVariant() {
    UUID id = UuidV7.generate();
    assertEquals(7, id.version());
    assertEquals(2, id.variant());
  }

  @Test
  public void embedsCurrentTimestamp() {
    long before = System.currentTimeMillis();
    UUID id = UuidV7.generate();
    long after = System.currentTimeMillis();
    long timestamp = id.getMostSignificantBits() >>> 16;
    assertTrue(timestamp >= before && timestamp <= after);
  }

  @Test
  public void generatesUniqueIdentifiers() {
    Set<UUID> ids = new HashSet<>();
    for (int i = 0; i < 10_000; i++) {
      assertTrue(ids.add(UuidV7.generate()));
    }
  }

  @Test
  public void keepsTimestampMonotonic() {
    long previous = 0;
    for (int i = 0; i < 100; i++) {
      long timestamp = UuidV7.generate().getMostSignificantBits() >>> 16;
      assertTrue(timestamp >= previous);
      previous = timestamp;
    }
  }
}
