package com.maritime.platform.common.redis.resilience;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis string implementation of {@link TtlCache}.
 */
public class RedisTtlCache implements TtlCache {

    private final StringRedisTemplate redis;
    private final String keyPrefix;

    public RedisTtlCache(StringRedisTemplate redis, String keyPrefix) {
        this.redis = redis;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public Optional<CachedValue> find(String key) {
        RedisSlidingWindowRateLimiter.requireKey(key);
        String redisKey = redisKey(key);
        String value = redis.opsForValue().get(redisKey);
        if (value == null) {
            return Optional.empty();
        }
        Long ttlMillis = redis.getExpire(redisKey, TimeUnit.MILLISECONDS);
        Duration remainingTtl = ttlMillis == null || ttlMillis < 0
                ? Duration.ZERO
                : Duration.ofMillis(ttlMillis);
        return Optional.of(new CachedValue(value, remainingTtl));
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        RedisSlidingWindowRateLimiter.requireKey(key);
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        redis.opsForValue().set(
                redisKey(key),
                value,
                RedisSlidingWindowRateLimiter.requirePositive(ttl, "ttl"));
    }

    @Override
    public void evict(String key) {
        RedisSlidingWindowRateLimiter.requireKey(key);
        redis.delete(redisKey(key));
    }

    private String redisKey(String key) {
        return keyPrefix + ":cache:" + key;
    }
}
