package com.maritime.platform.common.redis.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCommonPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void defaultsArePlatformScoped() {
        runner.run(ctx -> {
            RedisCommonProperties properties = ctx.getBean(RedisCommonProperties.class);

            assertThat(properties.getIdempotencyKeyPrefix()).isEqualTo("platform:idem");
            assertThat(properties.getLockKeyPrefix()).isEqualTo("platform:lock");
            assertThat(properties.getResilienceKeyPrefix()).isEqualTo("platform:resilience");
        });
    }

    @Test
    void prefixesCanBeOverriddenByConfiguration() {
        runner.withPropertyValues(
                "maritime.redis.idempotency-key-prefix=custom:idem",
                "maritime.redis.lock-key-prefix=custom:lock",
                "maritime.redis.resilience-key-prefix=custom:resilience"
        ).run(ctx -> {
            RedisCommonProperties properties = ctx.getBean(RedisCommonProperties.class);

            assertThat(properties.getIdempotencyKeyPrefix()).isEqualTo("custom:idem");
            assertThat(properties.getLockKeyPrefix()).isEqualTo("custom:lock");
            assertThat(properties.getResilienceKeyPrefix()).isEqualTo("custom:resilience");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RedisCommonProperties.class)
    static class TestConfiguration {
    }
}
