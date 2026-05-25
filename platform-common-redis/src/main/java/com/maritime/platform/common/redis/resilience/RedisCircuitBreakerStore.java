package com.maritime.platform.common.redis.resilience;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Redis implementation of {@link CircuitBreakerStore}.
 */
public class RedisCircuitBreakerStore implements CircuitBreakerStore {

    private final StringRedisTemplate redis;
    private final String keyPrefix;

    public RedisCircuitBreakerStore(StringRedisTemplate redis, String keyPrefix) {
        this.redis = redis;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public Optional<OpenCircuit> findOpenCircuit(String key, Instant now) {
        RedisSlidingWindowRateLimiter.requireKey(key);
        Instant effectiveNow = now == null ? Instant.now() : now;
        String openUntil = redis.opsForValue().get(openKey(key));
        if (openUntil == null) {
            return Optional.empty();
        }
        Instant until = Instant.ofEpochMilli(Long.parseLong(openUntil));
        return until.isAfter(effectiveNow) ? Optional.of(new OpenCircuit(until)) : Optional.empty();
    }

    @Override
    public void recordFailure(String key, int failureThreshold, Duration openDuration, Instant now) {
        RedisSlidingWindowRateLimiter.requireKey(key);
        Duration effectiveDuration = RedisSlidingWindowRateLimiter.requirePositive(openDuration, "openDuration");
        Instant effectiveNow = now == null ? Instant.now() : now;
        Long failures = redis.opsForValue().increment(failureKey(key));
        redis.expire(failureKey(key), effectiveDuration);
        if (failures != null && failures >= Math.max(1, failureThreshold)) {
            redis.opsForValue().set(
                    openKey(key),
                    String.valueOf(effectiveNow.plus(effectiveDuration).toEpochMilli()),
                    effectiveDuration);
        }
    }

    @Override
    public void recordSuccess(String key) {
        RedisSlidingWindowRateLimiter.requireKey(key);
        redis.delete(List.of(failureKey(key), openKey(key)));
    }

    private String failureKey(String key) {
        return keyPrefix + ":circuit:failures:" + key;
    }

    private String openKey(String key) {
        return keyPrefix + ":circuit:open:" + key;
    }
}
