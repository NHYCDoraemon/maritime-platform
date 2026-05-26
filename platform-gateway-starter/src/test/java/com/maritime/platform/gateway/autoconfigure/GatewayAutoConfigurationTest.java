package com.maritime.platform.gateway.autoconfigure;

import com.maritime.platform.gateway.error.DefaultGatewayErrorWriter;
import com.maritime.platform.gateway.error.GatewayErrorWriter;
import com.maritime.platform.gateway.security.AuthMode;
import com.maritime.platform.gateway.security.GatewaySecurityPolicyCustomizer;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GatewayAutoConfiguration tests")
class GatewayAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(GatewayAutoConfiguration.class))
            .withPropertyValues("spring.cloud.gateway.enabled=false",
                    "spring.main.web-application-type=none",
                    "maritime.gateway.security.default-auth-mode=none");

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

    @Nested
    @DisplayName("Fail-closed startup validation")
    class FailClosedStartup {

        @Test
        @DisplayName("context fails when default-auth-mode=JWT but jwt.enabled=false")
        void defaultJwtWithoutJwtEnabledFails() {
            runner.withPropertyValues(
                    "maritime.gateway.security.default-auth-mode=jwt"
            ).run(ctx -> {
                assertThat(ctx).getFailure().isNotNull();
                assertThat(ctx.getStartupFailure())
                        .hasMessageContaining("default-auth-mode")
                        .hasMessageContaining("JWT")
                        .hasMessageContaining("jwt.enabled");
            });
        }

        @Test
        @DisplayName("context fails when default-auth-mode=HMAC but hmac.enabled=false")
        void defaultHmacWithoutHmacEnabledFails() {
            runner.withPropertyValues(
                    "maritime.gateway.security.default-auth-mode=hmac"
            ).run(ctx -> {
                assertThat(ctx).getFailure().isNotNull();
                assertThat(ctx.getStartupFailure())
                        .hasMessageContaining("default-auth-mode")
                        .hasMessageContaining("HMAC")
                        .hasMessageContaining("hmac.enabled");
            });
        }

        @Test
        @DisplayName("context fails when default-auth-mode=JWT_OR_HMAC without both enabled")
        void defaultJwtOrHmacWithoutBothEnabledFails() {
            runner.withPropertyValues(
                    "maritime.gateway.security.default-auth-mode=jwt-or-hmac"
            ).run(ctx -> {
                assertThat(ctx).getFailure().isNotNull();
                assertThat(ctx.getStartupFailure())
                        .hasMessageContaining("JWT_OR_HMAC");
            });
        }

        @Test
        @DisplayName("context fails when route auth-mode=JWT but jwt.enabled=false")
        void routeJwtWithoutJwtEnabledFails() {
            runner.withPropertyValues(
                    "maritime.gateway.security.routes[0].id=my-api",
                    "maritime.gateway.security.routes[0].paths[0]=/api/**",
                    "maritime.gateway.security.routes[0].auth-mode=jwt"
            ).run(ctx -> {
                assertThat(ctx).getFailure().isNotNull();
                assertThat(ctx.getStartupFailure())
                        .hasMessageContaining("route 'my-api'")
                        .hasMessageContaining("jwt.enabled");
            });
        }

        @Test
        @DisplayName("context fails when route auth-mode=HMAC but hmac.enabled=false")
        void routeHmacWithoutHmacEnabledFails() {
            runner.withPropertyValues(
                    "maritime.gateway.security.routes[0].id=open-api",
                    "maritime.gateway.security.routes[0].paths[0]=/openapi/**",
                    "maritime.gateway.security.routes[0].auth-mode=hmac"
            ).run(ctx -> {
                assertThat(ctx).getFailure().isNotNull();
                assertThat(ctx.getStartupFailure())
                        .hasMessageContaining("route 'open-api'")
                        .hasMessageContaining("hmac.enabled");
            });
        }

        @Test
        @DisplayName("context succeeds with default-auth-mode=NONE and no filters enabled")
        void defaultNoneWithoutFiltersSucceeds() {
            runner.run(ctx -> {
                assertThat(ctx).hasNotFailed();
                assertThat(ctx).hasSingleBean(GatewaySecurityProperties.class);
            });
        }

        @Test
        @DisplayName("context fails when customizer adds JWT programmatic route but jwt.enabled=false")
        void customizerJwtRouteWithoutJwtEnabledFails() {
            runner.withUserConfiguration(CustomizerAddsJwtRoute.class).run(ctx -> {
                assertThat(ctx).getFailure().isNotNull();
                assertThat(ctx.getStartupFailure())
                        .hasMessageContaining("programmatic route")
                        .hasMessageContaining("prog-jwt")
                        .hasMessageContaining("jwt.enabled");
            });
        }

        @Test
        @DisplayName("context fails when customizer adds HMAC programmatic route but hmac.enabled=false")
        void customizerHmacRouteWithoutHmacEnabledFails() {
            runner.withUserConfiguration(CustomizerAddsHmacRoute.class).run(ctx -> {
                assertThat(ctx).getFailure().isNotNull();
                assertThat(ctx.getStartupFailure())
                        .hasMessageContaining("programmatic route")
                        .hasMessageContaining("prog-hmac")
                        .hasMessageContaining("hmac.enabled");
            });
        }

        @Test
        @DisplayName("context fails when customizer adds JWT_OR_HMAC programmatic route without both enabled")
        void customizerJwtOrHmacRouteWithoutBothEnabledFails() {
            runner.withUserConfiguration(CustomizerAddsJwtOrHmacRoute.class).run(ctx -> {
                assertThat(ctx).getFailure().isNotNull();
                assertThat(ctx.getStartupFailure())
                        .hasMessageContaining("programmatic route")
                        .hasMessageContaining("prog-dual")
                        .hasMessageContaining("JWT_OR_HMAC");
            });
        }

        @TestConfiguration(proxyBeanMethods = false)
        static class CustomizerAddsJwtRoute {
            @Bean
            GatewaySecurityPolicyCustomizer jwtCustomizer() {
                return resolver -> resolver.addRoutePolicy("prog-jwt",
                        List.of("/prog/**"), null, AuthMode.JWT);
            }
        }

        @TestConfiguration(proxyBeanMethods = false)
        static class CustomizerAddsHmacRoute {
            @Bean
            GatewaySecurityPolicyCustomizer hmacCustomizer() {
                return resolver -> resolver.addRoutePolicy("prog-hmac",
                        List.of("/prog/**"), null, AuthMode.HMAC);
            }
        }

        @TestConfiguration(proxyBeanMethods = false)
        static class CustomizerAddsJwtOrHmacRoute {
            @Bean
            GatewaySecurityPolicyCustomizer dualCustomizer() {
                return resolver -> resolver.addRoutePolicy("prog-dual",
                        List.of("/prog/**"), null, AuthMode.JWT_OR_HMAC);
            }
        }
    }
}
