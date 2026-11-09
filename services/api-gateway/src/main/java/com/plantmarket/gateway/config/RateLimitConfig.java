package com.plantmarket.gateway.config;

import com.plantmarket.gateway.web.InMemoryRateLimitStore;
import com.plantmarket.gateway.web.RateLimitStore;
import com.plantmarket.gateway.web.RedisRateLimitStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Configuration
public class RateLimitConfig {

  @Bean
  public RateLimitStore rateLimitStore(
      @Value("${app.rate-limit.redis-url:}") String redisUrl,
      ReactiveStringRedisTemplate redisTemplate) {
    return redisUrl.isBlank()
        ? new InMemoryRateLimitStore()
        : new RedisRateLimitStore(redisTemplate);
  }
}
