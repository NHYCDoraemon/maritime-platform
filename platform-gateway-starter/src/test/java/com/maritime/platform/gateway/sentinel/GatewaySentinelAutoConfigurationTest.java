package com.maritime.platform.gateway.sentinel;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.maritime.platform.gateway.autoconfigure.GatewayAutoConfiguration;
import com.maritime.platform.gateway.autoconfigure.GatewayRedisConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.List;

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

    @AfterEach
    void resetBlockHandler() {
        GatewayCallbackManager.resetBlockHandler();
    }

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

        @Test
        @DisplayName("block handler returns 429 with valid JSON body")
        void blockHandlerReturns429WithJsonBody() {
            runner
                    .withUserConfiguration(CodecConfigurerConfig.class)
                    .withPropertyValues(
                            "maritime.gateway.sentinel.enabled=true",
                            "spring.autoconfigure.exclude="
                                    + "com.alibaba.cloud.sentinel.gateway.scg.SentinelSCGAutoConfiguration,"
                                    + "com.alibaba.cloud.sentinel.gateway.SentinelGatewayAutoConfiguration")
                    .run(ctx -> {
                        BlockRequestHandler handler = GatewayCallbackManager.getBlockHandler();
                        assertThat(handler).isNotNull();

                        MockServerWebExchange exchange = MockServerWebExchange.from(
                                MockServerHttpRequest.get("/api/test"));

                        ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();
                        ServerResponse.Context context = new ServerResponse.Context() {
                            @Override
                            public List<HttpMessageWriter<?>> messageWriters() {
                                return codecConfigurer.getWriters();
                            }

                            @Override
                            public List<ViewResolver> viewResolvers() {
                                return List.of();
                            }
                        };

                        handler.handleRequest(exchange, new RuntimeException("blocked"))
                                .flatMap(response -> response.writeTo(exchange, context))
                                .block();

                        MockServerHttpResponse response = exchange.getResponse();
                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                        assertThat(response.getHeaders().getContentType())
                                .isEqualTo(MediaType.APPLICATION_JSON);

                        String body = response.getBodyAsString().block();
                        assertThat(body).contains("\"code\":429");
                        assertThat(body).contains("\"message\":\"FLOW_LIMITING\"");
                        assertThat(body).contains("\"data\":null");
                    });
        }

        @Test
        @DisplayName("block handler does not throw when constructing response body")
        void blockHandlerDoesNotThrow() {
            runner
                    .withUserConfiguration(CodecConfigurerConfig.class)
                    .withPropertyValues(
                            "maritime.gateway.sentinel.enabled=true",
                            "spring.autoconfigure.exclude="
                                    + "com.alibaba.cloud.sentinel.gateway.scg.SentinelSCGAutoConfiguration,"
                                    + "com.alibaba.cloud.sentinel.gateway.SentinelGatewayAutoConfiguration")
                    .run(ctx -> {
                        BlockRequestHandler handler = GatewayCallbackManager.getBlockHandler();
                        assertThat(handler).isNotNull();

                        MockServerWebExchange exchange = MockServerWebExchange.from(
                                MockServerHttpRequest.get("/api/test"));

                        ServerResponse serverResponse = handler.handleRequest(
                                exchange, new RuntimeException("blocked")).block();

                        assertThat(serverResponse).isNotNull();
                        assertThat(serverResponse.statusCode())
                                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
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
