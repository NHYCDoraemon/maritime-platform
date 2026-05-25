package com.maritime.platform.common.redis.resilience;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Domain-neutral circuit state store.
 */
public interface CircuitBreakerStore {

    Optional<OpenCircuit> findOpenCircuit(String key, Instant now);

    void recordFailure(String key, int failureThreshold, Duration openDuration, Instant now);

    void recordSuccess(String key);

    record OpenCircuit(Instant openUntil) {
        public OpenCircuit {
            if (openUntil == null) {
                throw new IllegalArgumentException("openUntil cannot be null");
            }
        }
    }
}
