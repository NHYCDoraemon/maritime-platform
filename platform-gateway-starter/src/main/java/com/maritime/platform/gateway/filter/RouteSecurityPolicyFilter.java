package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.security.RouteSecurityPolicy;
import com.maritime.platform.gateway.security.RouteSecurityPolicyResolver;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Resolves the {@link RouteSecurityPolicy} for every request and stores it
 * in an exchange attribute so downstream filters and handlers can make
 * auth decisions without re-resolving.
 * <p>
 * Passing the request through to the downstream chain unconditionally;
 * the auth-mode enforcement itself is implemented in later tasks.
 */
@Order(GatewayFilterOrder.SECURITY_POLICY)
public class RouteSecurityPolicyFilter implements GlobalFilter, Ordered {

	public static final String POLICY_ATTR = "gateway.securityPolicy";

	private final RouteSecurityPolicyResolver resolver;

	public RouteSecurityPolicyFilter(RouteSecurityPolicyResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		RouteSecurityPolicy policy = resolver.resolve(exchange.getRequest());
		exchange.getAttributes().put(POLICY_ATTR, policy);
		return chain.filter(exchange);
	}

	@Override
	public int getOrder() {
		return GatewayFilterOrder.SECURITY_POLICY;
	}
}
