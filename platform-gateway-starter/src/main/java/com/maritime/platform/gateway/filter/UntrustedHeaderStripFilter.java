package com.maritime.platform.gateway.filter;

import java.util.Set;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Strips headers that must only be set by the gateway, never trusted from clients.
 * <p>
 * Runs for all requests &mdash; including public paths &mdash; before any
 * auth or logging filter sees the request, so no downstream component can be
 * tricked by client-supplied internal headers.
 */
@Component
@Order(GatewayFilterOrder.UNTRUSTED_HEADER_STRIP)
public class UntrustedHeaderStripFilter implements GlobalFilter, Ordered {

	static final Set<String> UNTRUSTED_HEADERS = Set.of(
			"X-Internal-Call",
			"X-User-Id", "X-User-Name",
			"X-Active-Org-Code", "X-Active-Org-Name",
			"X-Session-Id",
			"X-System-Scope",
			"X-User-Source",
			"X-Tenant-Id", "X-Tenant-Code",
			"X-App-Key", "X-App-Code", "X-App-Id",
			"X-Verified-App-Code",
			"X-App-Permissions",
			"X-Trace-Id"
	);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest mutated = exchange.getRequest().mutate()
				.headers(headers -> UNTRUSTED_HEADERS.forEach(headers::remove))
				.build();
		return chain.filter(exchange.mutate().request(mutated).build());
	}

	@Override
	public int getOrder() {
		return GatewayFilterOrder.UNTRUSTED_HEADER_STRIP;
	}
}
