package com.maritime.platform.common.redis.resilience;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis sorted-set implementation of {@link SlidingWindowRateLimiter}.
 */
public class RedisSlidingWindowRateLimiter implements SlidingWindowRateLimiter {

    private static final String RATE_SCRIPT = """
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
            local current = redis.call('ZCARD', KEYS[1])
            if current >= tonumber(ARGV[2]) then
              return 0
            end
            redis.call('ZADD', KEYS[1], ARGV[3], ARGV[4])
            redis.call('PEXPIRE', KEYS[1], ARGV[5])
            return 1
            """;

    private final StringRedisTemplate redis;
    private final String keyPrefix;
    private final DefaultRedisScript<Long> rateScript;

    public RedisSlidingWindowRateLimiter(StringRedisTemplate redis, String keyPrefix) {
        this.redis = redis;
        this.keyPrefix = keyPrefix;
        this.rateScript = new DefaultRedisScript<>(RATE_SCRIPT, Long.class);
    }

    @Override
    public boolean tryAcquire(String key, int limit, Duration window, Instant now) {
        if (limit <= 0) {
            return true;
        }
        requireKey(key);
        Duration effectiveWindow = requirePositive(window, "window");
        Instant effectiveNow = now == null ? Instant.now() : now;
        long nowMillis = effectiveNow.toEpochMilli();
        long windowMillis = effectiveWindow.toMillis();
        Long acquired = redis.execute(rateScript, List.of(redisKey(key)),
                String.valueOf(nowMillis - windowMillis),
                String.valueOf(limit),
                String.valueOf(nowMillis),
                nowMillis + ":" + ThreadLocalRandom.current().nextLong(),
                String.valueOf(windowMillis));
        return Long.valueOf(1L).equals(acquired);
    }

    private String redisKey(String key) {
        return keyPrefix + ":rate:" + key;
    }

    static void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key cannot be blank");
        }
    }

    static Duration requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }
}
