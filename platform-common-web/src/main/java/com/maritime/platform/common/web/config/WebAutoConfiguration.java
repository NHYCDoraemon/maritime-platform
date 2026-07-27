package com.maritime.platform.common.web.config;

import com.maritime.platform.common.web.advice.TraceIdResponseAdvice;
import com.maritime.platform.common.web.filter.GatewayUserContextFilter;
import com.maritime.platform.common.web.filter.RequestLogFilter;
import com.maritime.platform.common.web.filter.TraceIdFilter;
import com.maritime.platform.common.web.handler.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that registers Web layer beans (GlobalExceptionHandler,
 * TraceIdFilter, RequestLogFilter) in Spring Boot services.
 *
 * <p>Only activates for Servlet-based web applications.
 * Gateway (WebFlux) must provide its own reactive equivalents.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    RequestLogFilter requestLogFilter() {
        return new RequestLogFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    GatewayUserContextFilter gatewayUserContextFilter() {
        return new GatewayUserContextFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    TraceIdResponseAdvice traceIdResponseAdvice() {
        return new TraceIdResponseAdvice();
    }

    @Bean
    @ConditionalOnMissingBean
    GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
