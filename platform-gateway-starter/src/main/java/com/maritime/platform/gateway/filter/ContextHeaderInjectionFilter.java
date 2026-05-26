package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewayPrincipalHeaderCustomizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Injects verified identity headers into the downstream request after
 * authentication has completed.
 * <p>
 * Trusted headers are stripped earlier in the chain by
 * {@link UntrustedHeaderStripFilter}, so any headers written here are
 * guaranteed to originate from the gateway starter.
 */
@Component
@Order(GatewayFilterOrder.CONTEXT_HEADER_INJECTION)
public class ContextHeaderInjectionFilter implements GlobalFilter, Ordered {

	private final TrustedHeaderWriter trustedHeaderWriter;
	private final List<GatewayPrincipalHeaderCustomizer> customizers;

	public ContextHeaderInjectionFilter(TrustedHeaderWriter trustedHeaderWriter,
			@Autowired(required = false) List<GatewayPrincipalHeaderCustomizer> customizers) {
		this.trustedHeaderWriter = trustedHeaderWriter;
		this.customizers = customizers != null ? customizers : List.of();
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest.Builder builder = exchange.getRequest().mutate();

		// Always write sanitized X-Trace-Id to downstream request.
		// The client original was already captured by TraceIdGatewayFilter
		// and stripped by UntrustedHeaderStripFilter, so this is the
		// gateway-confirmed value for all paths (public, JWT, HMAC).
		String traceId = exchange.getAttribute(TraceIdGatewayFilter.TRACE_ID_ATTR);
		if (traceId != null) {
			builder.header(TraceIdGatewayFilter.TRACE_ID_HEADER, traceId);
		}

		GatewayPrincipal principal = exchange.getAttribute(GatewayPrincipal.ATTRIBUTE);
		if (principal instanceof GatewayPrincipal.User user) {
			trustedHeaderWriter.writeUserHeaders(builder, user);
		} else if (principal instanceof GatewayPrincipal.App app) {
			trustedHeaderWriter.writeAppHeaders(builder, app);
		}

		if (principal != null) {
			for (GatewayPrincipalHeaderCustomizer customizer : customizers) {
				customizer.customize(builder, principal);
			}
		}

		ServerHttpRequest mutated = builder.build();
		return chain.filter(exchange.mutate().request(mutated).build());
	}

	@Override
	public int getOrder() {
		return GatewayFilterOrder.CONTEXT_HEADER_INJECTION;
	}
}
