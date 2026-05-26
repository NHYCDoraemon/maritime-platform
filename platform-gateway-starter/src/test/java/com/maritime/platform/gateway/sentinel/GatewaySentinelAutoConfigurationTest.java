package com.maritime.platform.gateway.sentinel;

import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.maritime.platform.gateway.autoconfigure.GatewayAutoConfiguration;
import com.maritime.platform.gateway.autoconfigure.GatewayRedisConfiguration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.codec.ServerCodecConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GatewaySentinelAutoConfiguration tests")
class GatewaySentinelAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    GatewayAutoConfiguration.class,
                    GatewayRedisConfiguration.class,
                    GatewaySentinelAutoConfiguration.class))
            .withPropertyValues("spring.cloud.gateway.enabled=false",
                    "spring.main.web-application-type=none",
                    "maritime.gateway.security.default-auth-mode=none");

    @Nested
    @DisplayName("Without Sentinel on classpath")
    class WithoutSentinel {

        @Test
        @DisplayName("does not activate when Sentinel is not on classpath")
        void doesNotActivateWithoutSentinel() {
            runner
                    .withClassLoader(new org.springframework.boot.test.context.FilteredClassLoader(
                            "com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter"))
                    .run(ctx -> {
                        assertThat(ctx.containsBean("sentinelGatewayBlockExceptionHandler")).isFalse();
                        assertThat(ctx.getBeansOfType(GatewaySentinelProperties.class)).isEmpty();
                    });
        }
    }

    @Nested
    @DisplayName("With Sentinel on classpath but disabled")
    class SentinelOnClasspathDisabled {

        @Test
        @DisplayName("does not activate when property is false (default)")
        void doesNotActivateWhenDisabled() {
            runner
                    .withPropertyValues(
                            "spring.autoconfigure.exclude="
                                    + "com.alibaba.cloud.sentinel.gateway.scg.SentinelSCGAutoConfiguration,"
                                    + "com.alibaba.cloud.sentinel.gateway.SentinelGatewayAutoConfiguration")
                    .run(ctx -> {
                        assertThat(ctx.containsBean("sentinelGatewayBlockExceptionHandler")).isFalse();
                    });
        }
    }

    @Nested
    @DisplayName("With Sentinel on classpath and enabled")
    class SentinelOnClasspathEnabled {

        @Test
        @DisplayName("activates when property is true")
        void activatesWhenEnabled() {
            runner
                    .withUserConfiguration(CodecConfigurerConfig.class)
                    .withPropertyValues(
                            "maritime.gateway.sentinel.enabled=true",
                            "spring.autoconfigure.exclude="
                                    + "com.alibaba.cloud.sentinel.gateway.scg.SentinelSCGAutoConfiguration,"
                                    + "com.alibaba.cloud.sentinel.gateway.SentinelGatewayAutoConfiguration")
                    .run(ctx -> {
                        assertThat(ctx).hasSingleBean(SentinelGatewayBlockExceptionHandler.class);
                        assertThat(ctx.getBean(GatewaySentinelProperties.class).isEnabled()).isTrue();
                    });
        }

        static class CodecConfigurerConfig {
            @Bean
            ServerCodecConfigurer serverCodecConfigurer() {
                return ServerCodecConfigurer.create();
            }
        }
    }
}
