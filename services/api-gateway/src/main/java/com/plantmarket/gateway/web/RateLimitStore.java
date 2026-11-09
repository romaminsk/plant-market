package com.plantmarket.gateway.web;

import java.time.Duration;
import reactor.core.publisher.Mono;

/** Fixed-window counter storage; Redis makes the window shared across gateway instances. */
public interface RateLimitStore {

  Mono<Window> increment(String key, Duration window);

  record Window(long count, long resetAfterMillis) {}
}
