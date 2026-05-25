package com.maritime.platform.common.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "maritime.redis")
public class RedisCommonProperties {

    public static final String DEFAULT_IDEMPOTENCY_KEY_PREFIX = "platform:idem";
    public static final String DEFAULT_LOCK_KEY_PREFIX = "platform:lock";
    public static final String DEFAULT_RESILIENCE_KEY_PREFIX = "platform:resilience";

    private String idempotencyKeyPrefix = DEFAULT_IDEMPOTENCY_KEY_PREFIX;
    private String lockKeyPrefix = DEFAULT_LOCK_KEY_PREFIX;
    private String resilienceKeyPrefix = DEFAULT_RESILIENCE_KEY_PREFIX;

    public String getIdempotencyKeyPrefix() {
        return idempotencyKeyPrefix;
    }

    public void setIdempotencyKeyPrefix(String idempotencyKeyPrefix) {
        this.idempotencyKeyPrefix = textOrDefault(
                idempotencyKeyPrefix,
                DEFAULT_IDEMPOTENCY_KEY_PREFIX);
    }

    public String getLockKeyPrefix() {
        return lockKeyPrefix;
    }

    public void setLockKeyPrefix(String lockKeyPrefix) {
        this.lockKeyPrefix = textOrDefault(lockKeyPrefix, DEFAULT_LOCK_KEY_PREFIX);
    }

    public String getResilienceKeyPrefix() {
        return resilienceKeyPrefix;
    }

    public void setResilienceKeyPrefix(String resilienceKeyPrefix) {
        this.resilienceKeyPrefix = textOrDefault(
                resilienceKeyPrefix,
                DEFAULT_RESILIENCE_KEY_PREFIX);
    }

    private static String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
