package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.error.GatewayErrorWriter;
import com.maritime.platform.gateway.security.AuthMode;
import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.RouteSecurityPolicy;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationManager;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Extracts and validates a JWT Bearer token for routes that require
 * JWT authentication. Stores the mapped {@link GatewayPrincipal.User}
 * in the exchange for downstream filters and handlers.
 */
@Order(GatewayFilterOrder.JWT_AUTHENTICATION)
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

	private final JwtAuthenticationManager authManager;
	private final GatewayErrorWriter errorWriter;

	public JwtAuthenticationGatewayFilter(JwtAuthenticationManager authManager,
			GatewayErrorWriter errorWriter) {
		this.authManager = authManager;
		this.errorWriter = errorWriter;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		RouteSecurityPolicy policy = exchange.getAttribute(RouteSecurityPolicyFilter.POLICY_ATTR);
		if (policy == null) {
			return chain.filter(exchange);
		}

		AuthMode mode = policy.getAuthMode();

		if (mode == AuthMode.JWT_AND_HMAC) {
			return errorWriter.write(exchange, GatewayAuthErrorCode.UNSUPPORTED_AUTH_MODE);
		}

		if (mode != AuthMode.JWT && mode != AuthMode.JWT_OR_HMAC) {
			return chain.filter(exchange);
		}

		String token = extractBearerToken(exchange);

		// JWT_OR_HMAC: only require JWT when Bearer token is present;
		// without a Bearer token, pass through to let HMAC handle it.
		if (mode == AuthMode.JWT_OR_HMAC && (token == null || token.isEmpty())) {
			return chain.filter(exchange);
		}

		if (token == null || token.isEmpty()) {
			return errorWriter.write(exchange, GatewayAuthErrorCode.MISSING_TOKEN);
		}

		return authManager.authenticate(token)
				.flatMap(principal -> {
					exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, principal);
					return chain.filter(exchange);
				})
				.onErrorResume(JwtAuthenticationException.class,
						e -> errorWriter.write(exchange, e.getErrorCode()));
	}

	@Override
	public int getOrder() {
		return GatewayFilterOrder.JWT_AUTHENTICATION;
	}

	private String extractBearerToken(ServerWebExchange exchange) {
		String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
		if (auth != null && auth.startsWith("Bearer ")) {
			return auth.substring(7).trim();
		}
		return null;
	}
}
