package com.maritime.platform.common.redis.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisConfigAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisConfig.class))
            .withBean(RedisConnectionFactory.class,
                    () -> mock(RedisConnectionFactory.class));

    @Test
    void createsDefaultRedisTemplate() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(RedisTemplate.class));
    }

    @Test
    void backsOffForConsumerRedisTemplate() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> custom = mock(RedisTemplate.class);

        runner.withBean("redisTemplate", RedisTemplate.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisTemplate.class);
                    assertThat(context.getBean("redisTemplate"))
                            .isSameAs(custom);
                });
    }
}
