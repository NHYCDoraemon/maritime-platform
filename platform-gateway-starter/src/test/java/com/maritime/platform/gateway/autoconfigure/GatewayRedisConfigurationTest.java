package com.maritime.platform.gateway.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GatewayRedisConfiguration tests")
class GatewayRedisConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GatewayRedisConfiguration.class)
            .withBean(ReactiveRedisConnectionFactory.class,
                    () -> org.mockito.Mockito.mock(LettuceConnectionFactory.class));

    @Test
    @DisplayName("creates ReactiveRedisTemplate<String,String> when Redis factory is available")
    void createsReactiveRedisTemplate() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(ReactiveRedisTemplate.class);
            ReactiveRedisTemplate<?, ?> template = ctx.getBean(ReactiveRedisTemplate.class);
            assertThat(template).isNotNull();
            assertThat(template.getConnectionFactory()).isNotNull();
        });
    }
}
