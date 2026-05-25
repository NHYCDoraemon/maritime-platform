package com.maritime.platform.gateway.autoconfigure;

import com.maritime.platform.gateway.error.DefaultGatewayErrorWriter;
import com.maritime.platform.gateway.error.GatewayErrorWriter;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GatewayAutoConfiguration tests")
class GatewayAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(GatewayAutoConfiguration.class))
            .withPropertyValues("spring.cloud.gateway.enabled=false",
                    "spring.main.web-application-type=none");

    @Nested
    @DisplayName("Default beans")
    class DefaultBeans {

        @Test
        @DisplayName("creates default GatewayErrorWriter")
        void createsDefaultErrorWriter() {
            runner.run(ctx -> {
                assertThat(ctx).hasSingleBean(GatewayErrorWriter.class);
                assertThat(ctx.getBean(GatewayErrorWriter.class))
                        .isInstanceOf(DefaultGatewayErrorWriter.class);
            });
        }

        @Test
        @DisplayName("component scan finds filter beans via AutoConfiguration")
        void createsFilterBeans() {
            runner.run(ctx -> {
                assertThat(ctx.containsBean("traceIdGatewayFilter")).isTrue();
                assertThat(ctx.containsBean("untrustedHeaderStripFilter")).isTrue();
                assertThat(ctx.containsBean("routeSecurityPolicyFilter")).isTrue();
                assertThat(ctx.containsBean("contextHeaderInjectionFilter")).isTrue();
                assertThat(ctx.containsBean("requestLogGatewayFilter")).isTrue();
            });
        }

        @Test
        @DisplayName("creates security resolver and properties beans")
        void createsSecurityBeans() {
            runner.run(ctx -> {
                assertThat(ctx.containsBean("routeSecurityPolicyResolver")).isTrue();
                assertThat(ctx.getBean(GatewaySecurityProperties.class)).isNotNull();
            });
        }
    }

    @Nested
    @DisplayName("Custom GatewayErrorWriter override")
    class CustomErrorWriter {

        @Test
        @DisplayName("custom GatewayErrorWriter overrides the default")
        void customWriterOverridesDefault() {
            runner.withUserConfiguration(CustomWriterConfig.class).run(ctx -> {
                assertThat(ctx).hasSingleBean(GatewayErrorWriter.class);
                assertThat(ctx.getBean(GatewayErrorWriter.class))
                        .isInstanceOf(CustomWriterConfig.TestErrorWriter.class);
            });
        }

        static class CustomWriterConfig {
            @Bean
            GatewayErrorWriter testErrorWriter() {
                return new TestErrorWriter();
            }

            static class TestErrorWriter implements GatewayErrorWriter {
                @Override
                public reactor.core.publisher.Mono<Void> write(
                        org.springframework.web.server.ServerWebExchange exchange, String errorCode) {
                    return reactor.core.publisher.Mono.empty();
                }
            }
        }
    }
}
