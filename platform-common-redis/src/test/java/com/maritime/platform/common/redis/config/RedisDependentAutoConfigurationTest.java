package com.maritime.platform.common.redis.config;

import com.maritime.platform.common.redis.idempotency.IdempotencyAutoConfiguration;
import com.maritime.platform.common.redis.idempotency.IdempotencyPort;
import com.maritime.platform.common.redis.leader.LeaderElectedAspect;
import com.maritime.platform.common.redis.leader.LeaderElectedAutoConfiguration;
import com.maritime.platform.common.redis.lock.DistributedLockAspect;
import com.maritime.platform.common.redis.lock.DistributedLockAutoConfiguration;
import com.maritime.platform.common.redis.lockport.LockPort;
import com.maritime.platform.common.redis.lockport.LockPortAutoConfiguration;
import com.maritime.platform.common.redis.resilience.CircuitBreakerStore;
import com.maritime.platform.common.redis.resilience.ResilienceAutoConfiguration;
import com.maritime.platform.common.redis.resilience.SlidingWindowRateLimiter;
import com.maritime.platform.common.redis.resilience.TtlCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisDependentAutoConfigurationTest {

    private final ApplicationContextRunner redisRunner =
            new ApplicationContextRunner()
                    .withConfiguration(redisAutoConfigurations())
                    .withBean(RedisConnectionFactory.class,
                            () -> mock(RedisConnectionFactory.class));

    private static AutoConfigurations redisAutoConfigurations() {
        return AutoConfigurations.of(
                DistributedLockAutoConfiguration.class,
                IdempotencyAutoConfiguration.class,
                LeaderElectedAutoConfiguration.class,
                LockPortAutoConfiguration.class,
                ResilienceAutoConfiguration.class,
                JacksonAutoConfiguration.class,
                RedisAutoConfiguration.class);
    }

    @Test
    void createsRedisBackedBeansAfterBootAutoConfigurations() {
        redisRunner.run(context -> assertThat(context)
                .hasSingleBean(StringRedisTemplate.class)
                .hasSingleBean(DistributedLockAspect.class)
                .hasSingleBean(LockPort.class)
                .hasSingleBean(LeaderElectedAspect.class)
                .hasSingleBean(IdempotencyPort.class)
                .hasSingleBean(SlidingWindowRateLimiter.class)
                .hasSingleBean(CircuitBreakerStore.class)
                .hasSingleBean(TtlCache.class));
    }

    @Test
    void backsOffCleanlyWithoutRedisConnectionFactory() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("io.lettuce.core"))
                .withConfiguration(redisAutoConfigurations())
                .run(context -> assertThat(context)
                        .doesNotHaveBean(StringRedisTemplate.class)
                        .doesNotHaveBean(DistributedLockAspect.class)
                        .doesNotHaveBean(LockPort.class)
                        .doesNotHaveBean(LeaderElectedAspect.class)
                        .doesNotHaveBean(IdempotencyPort.class)
                        .doesNotHaveBean(SlidingWindowRateLimiter.class)
                        .doesNotHaveBean(CircuitBreakerStore.class)
                        .doesNotHaveBean(TtlCache.class));
    }

    @Test
    void backsOffForConsumerLockAndIdempotencyPorts() {
        LockPort customLockPort = mock(LockPort.class);
        IdempotencyPort customIdempotencyPort = mock(IdempotencyPort.class);

        redisRunner
                .withBean(LockPort.class, () -> customLockPort)
                .withBean(IdempotencyPort.class, () -> customIdempotencyPort)
                .run(context -> {
                    assertThat(context).hasSingleBean(LockPort.class);
                    assertThat(context.getBean(LockPort.class))
                            .isSameAs(customLockPort);
                    assertThat(context).hasSingleBean(IdempotencyPort.class);
                    assertThat(context.getBean(IdempotencyPort.class))
                            .isSameAs(customIdempotencyPort);
                    assertThat(context).hasSingleBean(LeaderElectedAspect.class);
                });
    }
}
