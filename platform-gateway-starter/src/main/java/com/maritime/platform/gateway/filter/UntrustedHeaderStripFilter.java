package com.maritime.platform.gateway.filter;

import java.util.Set;

import com.maritime.platform.gateway.security.GatewaySecurityProperties;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Strips headers that must only be set by the gateway, never trusted from clients.
 * <p>
 * Runs for all requests &mdash; including public paths &mdash; before any
 * auth or logging filter sees the request, so no downstream component can be
 * tricked by client-supplied internal headers.
 * <p>
 * Configured HMAC signature headers are preserved so that
 * {@link HmacAuthenticationGatewayFilter} can read them before they are
 * stripped post-authentication.
 */
@Order(GatewayFilterOrder.UNTRUSTED_HEADER_STRIP)
public class UntrustedHeaderStripFilter implements GlobalFilter, Ordered {

	// Headers that must only be set by the gateway, never trusted from clients.
	// HMAC signature headers are handled separately: they are preserved during
	// the initial strip (so HMAC auth can read them) and removed after auth
	// decision in HmacAuthenticationGatewayFilter.
	static final Set<String> CONTEXT_HEADERS = Set.of(
			"X-Internal-Call",
			"X-User-Id", "X-User-Name",
			"X-Active-Org-Code", "X-Active-Org-Name",
			"X-Session-Id",
			"X-System-Scope",
			"X-User-Source",
			"X-Tenant-Id", "X-Tenant-Code",
			"X-App-Code", "X-App-Id",
			"X-Verified-App-Code",
			"X-App-Permissions",
			"X-User-Permissions",
			"X-Test-Channel",
			"X-Trace-Id"
	);

	private final GatewaySecurityProperties properties;

	public UntrustedHeaderStripFilter(GatewaySecurityProperties properties) {
		this.properties = properties;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		Set<String> hmacHeaders = hmacHeaderNames();
		ServerHttpRequest mutated = exchange.getRequest().mutate()
				.headers(headers -> {
					for (String h : CONTEXT_HEADERS) {
						if (!hmacHeaders.contains(h)) {
							headers.remove(h);
						}
					}
				})
				.build();
		return chain.filter(exchange.mutate().request(mutated).build());
	}

	/**
	 * Returns the set of currently configured HMAC signature header names.
	 * These are preserved during the untrusted-header strip so that the
	 * HMAC authentication filter can read them.
	 */
	private Set<String> hmacHeaderNames() {
		GatewaySecurityProperties.Headers hdrs = properties.getHmac().getHeaders();
		return Set.of(
				hdrs.getAppKey(),
				hdrs.getTimestamp(),
				hdrs.getNonce(),
				hdrs.getBodyDigest(),
				hdrs.getSignature()
		);
	}

	@Override
	public int getOrder() {
		return GatewayFilterOrder.UNTRUSTED_HEADER_STRIP;
	}
}
