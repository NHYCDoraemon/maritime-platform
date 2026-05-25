package com.maritime.platform.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Injects verified context headers (user / app identity) into the
 * downstream request after authentication has completed.
 * <p>
 * This first implementation is a pass-through placeholder;
 * full header injection is delivered in the context-injection task.
 */
@Component
@Order(GatewayFilterOrder.CONTEXT_HEADER_INJECTION)
public class ContextHeaderInjectionFilter implements GlobalFilter, Ordered {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		return chain.filter(exchange);
	}

	@Override
	public int getOrder() {
		return GatewayFilterOrder.CONTEXT_HEADER_INJECTION;
	}
}
