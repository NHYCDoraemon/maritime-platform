package com.maritime.platform.gateway.autoconfigure;

import com.maritime.platform.gateway.error.DefaultGatewayErrorWriter;
import com.maritime.platform.gateway.error.GatewayErrorWriter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration(before = org.springframework.cloud.gateway.config.GatewayAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.config.GatewayAutoConfiguration")
@ComponentScan(basePackages = {
		"com.maritime.platform.gateway.filter",
		"com.maritime.platform.gateway.security"
})
public class GatewayAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public GatewayErrorWriter gatewayErrorWriter() {
		return new DefaultGatewayErrorWriter();
	}
}
