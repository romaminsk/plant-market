package com.plantmarket.common.id;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generates RFC 9562 UUID version 7 identifiers: 48-bit Unix epoch milliseconds followed by random
 * bits.
 *
 * <p>UUIDv7 keeps identifiers time-ordered, which is friendlier to B-tree indexes than UUIDv4 while
 * remaining globally unique.
 */
public final class UuidV7 {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final long TIMESTAMP_MASK = 0xFFFF_FFFF_FFFFL;
  private static final long RANDOM_12_BITS_MASK = 0xFFFL;
  private static final long RANDOM_62_BITS_MASK = 0x3FFF_FFFF_FFFF_FFFFL;
  private static final long VERSION_7 = 0x7000L;
  private static final long VARIANT_10 = 0x8000_0000_0000_0000L;

  private UuidV7() {}

  public static UUID generate() {
    long timestamp = System.currentTimeMillis() & TIMESTAMP_MASK;
    long randomA = RANDOM.nextLong() & RANDOM_12_BITS_MASK;
    long randomB = RANDOM.nextLong() & RANDOM_62_BITS_MASK;

    long mostSignificantBits = (timestamp << 16) | VERSION_7 | randomA;
    long leastSignificantBits = VARIANT_10 | randomB;
    return new UUID(mostSignificantBits, leastSignificantBits);
  }
}
