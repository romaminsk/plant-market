package com.plantmarket.gateway.web;

import java.time.Duration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

/** Redis fixed window: INCR creates the key and the first request sets its TTL. */
public final class RedisRateLimitStore implements RateLimitStore {

  private final ReactiveStringRedisTemplate redisTemplate;
  private final RateLimitStore fallback = new InMemoryRateLimitStore();

  public RedisRateLimitStore(ReactiveStringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public Mono<Window> increment(String key, Duration window) {
    return redisTemplate
        .opsForValue()
        .increment(key)
        .flatMap(count -> {
          if (count == 1L) {
            return redisTemplate.expire(key, window).then(ttl(key, window, count));
          }
          return ttl(key, window, count);
        })
        .onErrorResume(error -> fallback.increment(key, window));
  }

  private Mono<Window> ttl(String key, Duration window, long count) {
    return redisTemplate
        .getExpire(key)
        .map(ttl ->
            new Window(
                count, ttl.isNegative() || ttl.isZero() ? window.toMillis() : ttl.toMillis()));
  }
}
