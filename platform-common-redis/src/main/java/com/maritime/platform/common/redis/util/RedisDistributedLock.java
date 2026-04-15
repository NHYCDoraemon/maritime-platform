package com.maritime.platform.common.redis.util;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Distributed lock with owner verification.
 *
 * <p>Uses Redis SETNX for acquisition and a Lua script
 * for atomic compare-and-delete on release. This prevents
 * a process from accidentally releasing another holder's lock.
 */
public final class RedisDistributedLock {

    private static final RedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get',KEYS[1])==ARGV[1] "
                            + "then return redis.call('del',KEYS[1]) "
                            + "else return 0 end",
                    Long.class);

    private RedisDistributedLock() {
    }

    /**
     * Generate a unique lock owner ID.
     */
    public static String newRequestId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Try to acquire a distributed lock.
     *
     * @return true if lock acquired
     */
    public static boolean tryLock(StringRedisTemplate redis,
                                  String key, String requestId,
                                  Duration ttl) {
        Boolean acquired = redis.opsForValue()
                .setIfAbsent(key, requestId, ttl);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * Release a distributed lock only if still held by this owner.
     *
     * @return true if lock was released, false if not held
     */
    public static boolean releaseLock(StringRedisTemplate redis,
                                      String key,
                                      String requestId) {
        Long result = redis.execute(RELEASE_SCRIPT,
                List.of(key), requestId);
        return Long.valueOf(1).equals(result);
    }
}
