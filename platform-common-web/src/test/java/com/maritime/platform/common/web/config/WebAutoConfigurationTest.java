package com.maritime.platform.common.web.config;

import com.maritime.platform.common.web.advice.TraceIdResponseAdvice;
import com.maritime.platform.common.web.filter.GatewayUserContextFilter;
import com.maritime.platform.common.web.filter.RequestLogFilter;
import com.maritime.platform.common.web.filter.TraceIdFilter;
import com.maritime.platform.common.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;

import static org.assertj.core.api.Assertions.assertThat;

class WebAutoConfigurationTest {

    private final WebApplicationContextRunner runner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            WebAutoConfiguration.class));

    @Test
    void explicitlyRegistersWebInfrastructure() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(TraceIdFilter.class);
            assertThat(context).hasSingleBean(RequestLogFilter.class);
            assertThat(context).hasSingleBean(GatewayUserContextFilter.class);
            assertThat(context).hasSingleBean(TraceIdResponseAdvice.class);
            assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
        });
    }

    @Test
    void doesNotUseComponentScanning() {
        assertThat(WebAutoConfiguration.class.getAnnotation(
                ComponentScan.class)).isNull();
    }

    @Test
    void backsOffForConsumerTraceIdFilter() {
        TraceIdFilter custom = new TraceIdFilter();

        runner.withBean("consumerTraceIdFilter", TraceIdFilter.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(TraceIdFilter.class);
                    assertThat(context.getBean(TraceIdFilter.class))
                            .isSameAs(custom);
                });
    }
}
