package com.maritime.platform.gateway.knife4j;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@ConditionalOnClass(name = "com.github.xiaoymin.knife4j.spring.gateway.Knife4jGatewayAutoConfiguration")
@ConditionalOnProperty(name = "maritime.gateway.knife4j.enabled", havingValue = "true")
@EnableConfigurationProperties(GatewayKnife4jProperties.class)
public class GatewayKnife4jAutoConfiguration {

    // Knife4j Gateway's own auto-configuration (registered via its
    // AutoConfiguration.imports) activates independently when the
    // dependency is on the classpath. This class serves as the
    // maritime-namespaced gatekeeper and configuration helper.
}
