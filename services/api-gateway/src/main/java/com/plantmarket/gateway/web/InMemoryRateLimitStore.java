package com.plantmarket.gateway.web;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import reactor.core.publisher.Mono;

/** Single-instance fallback used when no Redis URL is configured. */
public final class InMemoryRateLimitStore implements RateLimitStore {

  private static final int MAX_TRACKED_KEYS = 10_000;

  private final ConcurrentHashMap<String, Entry> windows = new ConcurrentHashMap<>();

  @Override
  public Mono<Window> increment(String key, Duration window) {
    long now = System.currentTimeMillis();
    long windowMillis = window.toMillis();
    Entry entry =
        windows.compute(
            key,
            (ignored, existing) ->
                existing == null || now - existing.startMillis >= windowMillis
                    ? new Entry(now)
                    : existing);
    long count = entry.count.incrementAndGet();
    if (windows.size() > MAX_TRACKED_KEYS) {
      windows
          .entrySet()
          .removeIf(candidate -> now - candidate.getValue().startMillis >= windowMillis);
    }
    long resetAfter = Math.max(1, windowMillis - (now - entry.startMillis));
    return Mono.just(new Window(count, resetAfter));
  }

  private static final class Entry {
    private final long startMillis;
    private final AtomicLong count = new AtomicLong();

    private Entry(long startMillis) {
      this.startMillis = startMillis;
    }
  }
}
