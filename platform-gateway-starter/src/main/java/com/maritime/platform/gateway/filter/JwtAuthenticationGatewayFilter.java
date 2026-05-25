package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.security.AuthMode;
import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.RouteSecurityPolicy;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Extracts and validates a JWT Bearer token for routes that require
 * JWT authentication. Stores the mapped {@link GatewayPrincipal.User}
 * in the exchange for downstream filters and handlers.
 */
@Component
@ConditionalOnProperty("maritime.gateway.security.jwt.enabled")
@Order(GatewayFilterOrder.JWT_AUTHENTICATION)
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

	private final JwtAuthenticationManager authManager;

	public JwtAuthenticationGatewayFilter(JwtAuthenticationManager authManager) {
		this.authManager = authManager;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		RouteSecurityPolicy policy = exchange.getAttribute(RouteSecurityPolicyFilter.POLICY_ATTR);
		if (!requiresJwt(policy)) {
			return chain.filter(exchange);
		}

		String token = extractBearerToken(exchange);
		if (token == null || token.isEmpty()) {
			return writeError(exchange, GatewayAuthErrorCode.MISSING_TOKEN,
					"Authorization header is missing or does not contain a Bearer token");
		}

		return authManager.authenticate(token)
				.flatMap(principal -> {
					exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, principal);
					return chain.filter(exchange);
				})
				.onErrorResume(JwtAuthenticationException.class,
						e -> writeError(exchange, e.getErrorCode(), e.getMessage()));
	}

	@Override
	public int getOrder() {
		return GatewayFilterOrder.JWT_AUTHENTICATION;
	}

	private boolean requiresJwt(RouteSecurityPolicy policy) {
		if (policy == null) {
			return false;
		}
		AuthMode mode = policy.getAuthMode();
		return mode == AuthMode.JWT || mode == AuthMode.JWT_OR_HMAC || mode == AuthMode.JWT_AND_HMAC;
	}

	private String extractBearerToken(ServerWebExchange exchange) {
		String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
		if (auth != null && auth.startsWith("Bearer ")) {
			return auth.substring(7).trim();
		}
		return null;
	}

	private Mono<Void> writeError(ServerWebExchange exchange, String code, String message) {
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
		String body = String.format("{\"code\":\"%s\",\"message\":\"%s\"}",
				escapeJson(code), escapeJson(message));
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		return exchange.getResponse()
				.writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
	}

	private static String escapeJson(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}
}
