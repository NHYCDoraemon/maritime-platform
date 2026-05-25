package com.maritime.platform.common.redis.resilience;

import java.time.Duration;
import java.util.Optional;

/**
 * Domain-neutral TTL cache for string payloads. Consumers own key naming and
 * payload serialization.
 */
public interface TtlCache {

    Optional<CachedValue> find(String key);

    void put(String key, String value, Duration ttl);

    void evict(String key);

    record CachedValue(String value, Duration remainingTtl) {
        public CachedValue {
            if (remainingTtl == null || remainingTtl.isNegative()) {
                remainingTtl = Duration.ZERO;
            }
        }
    }
}
