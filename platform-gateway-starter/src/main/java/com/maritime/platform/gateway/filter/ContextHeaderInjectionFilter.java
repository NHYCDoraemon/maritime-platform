package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewayPrincipalHeaderCustomizer;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Injects verified identity headers into the downstream request after
 * authentication has completed.
 * <p>
 * Trusted headers are stripped earlier in the chain by
 * {@link UntrustedHeaderStripFilter}, so any headers written here are
 * guaranteed to originate from the gateway starter.
 */
@Order(GatewayFilterOrder.CONTEXT_HEADER_INJECTION)
public class ContextHeaderInjectionFilter implements GlobalFilter, Ordered {

	private final TrustedHeaderWriter trustedHeaderWriter;
	private final List<GatewayPrincipalHeaderCustomizer> customizers;
	private final GatewaySecurityProperties properties;

	/**
	 * Default HMAC signature header names that must always be stripped
	 * before forwarding, regardless of whether HMAC is enabled or how
	 * headers are configured.
	 */
	static final Set<String> DEFAULT_HMAC_HEADERS = Set.of(
			"X-App-Key", "X-Timestamp", "X-Nonce", "X-Body-Digest", "X-Signature");

	public ContextHeaderInjectionFilter(TrustedHeaderWriter trustedHeaderWriter,
			GatewaySecurityProperties properties,
			List<GatewayPrincipalHeaderCustomizer> customizers) {
		this.trustedHeaderWriter = trustedHeaderWriter;
		this.properties = properties;
		this.customizers = customizers != null ? customizers : List.of();
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest.Builder builder = exchange.getRequest().mutate();

		// Strip all known raw HMAC signature headers before forwarding.
		// This runs unconditionally for all paths and auth modes, so
		// raw signature material never reaches downstream even when
		// hmac.enabled=false or the HMAC filter is absent.
		Set<String> allHmacHeaders = allHmacHeaderNames();
		if (!allHmacHeaders.isEmpty()) {
			builder.headers(headers -> {
				for (String h : allHmacHeaders) {
					headers.remove(h);
				}
			});
		}

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

	/**
	 * Returns the union of default and currently configured HMAC signature
	 * header names. These are stripped unconditionally before forwarding.
	 */
	private Set<String> allHmacHeaderNames() {
		Set<String> names = new LinkedHashSet<>(DEFAULT_HMAC_HEADERS);
		if (properties != null) {
			GatewaySecurityProperties.Headers hdrs = properties.getHmac().getHeaders();
			names.add(hdrs.getAppKey());
			names.add(hdrs.getTimestamp());
			names.add(hdrs.getNonce());
			names.add(hdrs.getBodyDigest());
			names.add(hdrs.getSignature());
		}
		return names;
	}

	@Override
	public int getOrder() {
		return GatewayFilterOrder.CONTEXT_HEADER_INJECTION;
	}
}
