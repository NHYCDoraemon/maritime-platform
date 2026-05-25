package com.maritime.platform.common.redis.resilience;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisResiliencePrimitivesTest {

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    private static StringRedisTemplate redisTemplate;
    private static SlidingWindowRateLimiter rateLimiter;
    private static CircuitBreakerStore circuitBreakerStore;
    private static TtlCache ttlCache;

    @BeforeAll
    static void setUp() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        factory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.afterPropertiesSet();

        rateLimiter = new RedisSlidingWindowRateLimiter(redisTemplate, "test:resilience");
        circuitBreakerStore = new RedisCircuitBreakerStore(redisTemplate, "test:resilience");
        ttlCache = new RedisTtlCache(redisTemplate, "test:resilience");
    }

    @BeforeEach
    void cleanup() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void slidingWindowRateLimiter_rejectsRequestsOverLimitUntilWindowMoves() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        assertThat(rateLimiter.tryAcquire("client-A", 2, Duration.ofMinutes(1), now)).isTrue();
        assertThat(rateLimiter.tryAcquire("client-A", 2, Duration.ofMinutes(1), now.plusSeconds(1))).isTrue();
        assertThat(rateLimiter.tryAcquire("client-A", 2, Duration.ofMinutes(1), now.plusSeconds(2))).isFalse();
        assertThat(rateLimiter.tryAcquire("client-A", 2, Duration.ofMinutes(1), now.plusSeconds(61))).isTrue();
    }

    @Test
    void circuitBreakerStore_opensAfterThresholdAndCanBeReset() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        circuitBreakerStore.recordFailure("service-A", 2, Duration.ofMinutes(1), now);
        assertThat(circuitBreakerStore.findOpenCircuit("service-A", now)).isEmpty();

        circuitBreakerStore.recordFailure("service-A", 2, Duration.ofMinutes(1), now.plusSeconds(1));
        assertThat(circuitBreakerStore.findOpenCircuit("service-A", now.plusSeconds(2)))
                .get()
                .extracting(CircuitBreakerStore.OpenCircuit::openUntil)
                .isEqualTo(now.plusSeconds(61));

        assertThat(circuitBreakerStore.findOpenCircuit("service-A", now.plusSeconds(90))).isEmpty();

        circuitBreakerStore.recordSuccess("service-A");
        assertThat(circuitBreakerStore.findOpenCircuit("service-A", now.plusSeconds(2))).isEmpty();
    }

    @Test
    void ttlCache_storesStringPayloadUntilTtlExpires() throws Exception {
        ttlCache.put("payload-A", "plain-json-or-any-string", Duration.ofMillis(300));

        assertThat(ttlCache.find("payload-A"))
                .get()
                .extracting(TtlCache.CachedValue::value)
                .isEqualTo("plain-json-or-any-string");

        Thread.sleep(500);

        assertThat(ttlCache.find("payload-A")).isEmpty();
    }
}
