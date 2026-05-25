package com.maritime.platform.gateway.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration(before = org.springframework.cloud.gateway.config.GatewayAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.config.GatewayAutoConfiguration")
@ComponentScan(basePackages = "com.maritime.platform.gateway")
public class GatewayAutoConfiguration {
}
