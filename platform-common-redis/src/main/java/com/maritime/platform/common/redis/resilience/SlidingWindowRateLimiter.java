package com.maritime.platform.common.redis.resilience;

import java.time.Duration;
import java.time.Instant;

/**
 * Domain-neutral sliding-window rate limiter.
 */
public interface SlidingWindowRateLimiter {

    /**
     * Attempts to acquire one slot for {@code key}.
     *
     * @param key logical caller/resource key, interpreted only by the consumer
     * @param limit max events allowed within {@code window}; values <= 0 mean unlimited
     * @param window sliding window duration
     * @param now caller-provided clock value for deterministic tests and replay
     * @return true when the slot was acquired, false when the key is rate limited
     */
    boolean tryAcquire(String key, int limit, Duration window, Instant now);
}
