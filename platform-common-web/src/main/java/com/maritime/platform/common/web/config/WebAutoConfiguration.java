package com.maritime.platform.common.web.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration that registers Web layer beans (GlobalExceptionHandler,
 * TraceIdFilter, RequestLogFilter) in Spring Boot services.
 *
 * <p>Only activates for Servlet-based web applications.
 * Gateway (WebFlux) must provide its own reactive equivalents.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ComponentScan(basePackages = "com.maritime.platform.common.web")
public class WebAutoConfiguration {
}
