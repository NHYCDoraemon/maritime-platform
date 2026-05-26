package com.maritime.platform.gateway.knife4j;

import com.maritime.platform.gateway.autoconfigure.GatewayAutoConfiguration;
import com.maritime.platform.gateway.autoconfigure.GatewayRedisConfiguration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GatewayKnife4jAutoConfiguration tests")
class GatewayKnife4jAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    GatewayAutoConfiguration.class,
                    GatewayRedisConfiguration.class,
                    GatewayKnife4jAutoConfiguration.class))
            .withPropertyValues("spring.cloud.gateway.enabled=false",
                    "spring.main.web-application-type=none",
                    "maritime.gateway.security.default-auth-mode=none");

    @Nested
    @DisplayName("Without Knife4j on classpath")
    class WithoutKnife4j {

        @Test
        @DisplayName("does not activate when Knife4j is not on classpath")
        void doesNotActivateWithoutKnife4j() {
            runner
                    .withClassLoader(new org.springframework.boot.test.context.FilteredClassLoader(
                            "com.github.xiaoymin.knife4j.spring.gateway.Knife4jGatewayAutoConfiguration"))
                    .run(ctx -> {
                        assertThat(ctx.getBeansOfType(GatewayKnife4jProperties.class)).isEmpty();
                    });
        }
    }

    @Nested
    @DisplayName("With Knife4j on classpath but disabled")
    class Knife4jOnClasspathDisabled {

        @Test
        @DisplayName("does not activate when property is false (default)")
        void doesNotActivateWhenDisabled() {
            runner
                    .withPropertyValues(
                            "spring.autoconfigure.exclude="
                                    + "com.github.xiaoymin.knife4j.spring.gateway.Knife4jGatewayAutoConfiguration")
                    .run(ctx -> {
                        assertThat(ctx.getBeansOfType(GatewayKnife4jProperties.class)).isEmpty();
                    });
        }
    }

    @Nested
    @DisplayName("With Knife4j on classpath and enabled")
    class Knife4jOnClasspathEnabled {

        @Test
        @DisplayName("activates when property is true")
        void activatesWhenEnabled() {
            runner
                    .withPropertyValues(
                            "maritime.gateway.knife4j.enabled=true",
                            "spring.autoconfigure.exclude="
                                    + "com.github.xiaoymin.knife4j.spring.gateway.Knife4jGatewayAutoConfiguration")
                    .run(ctx -> {
                        assertThat(ctx.getBean(GatewayKnife4jProperties.class).isEnabled()).isTrue();
                        assertThat(ctx).hasSingleBean(GatewayKnife4jProperties.class);
                    });
        }
    }
}
