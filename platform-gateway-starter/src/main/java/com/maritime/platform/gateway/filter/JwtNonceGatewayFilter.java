package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.security.AuthMode;
import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.RouteSecurityPolicy;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;
import com.maritime.platform.gateway.security.nonce.JwtNonceValidator;

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
 * Enforces JWT nonce validation for write requests on JWT-protected routes.
 * <p>
 * Runs after {@link JwtAuthenticationGatewayFilter} so the authenticated
 * principal is available. Only enforces nonce for HTTP methods configured
 * in {@code maritime.gateway.security.jwt.nonce.required-methods}
 * (default: POST, PUT, PATCH, DELETE).
 */
@Component
@ConditionalOnProperty("maritime.gateway.security.jwt.enabled")
@Order(GatewayFilterOrder.JWT_NONCE)
public class JwtNonceGatewayFilter implements GlobalFilter, Ordered {

	private final JwtNonceValidator nonceValidator;
	private final GatewaySecurityProperties properties;

	public JwtNonceGatewayFilter(JwtNonceValidator nonceValidator,
			GatewaySecurityProperties properties) {
		this.nonceValidator = nonceValidator;
		this.properties = properties;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		if (!properties.getJwt().getNonce().isEnabled()) {
			return chain.filter(exchange);
		}

		RouteSecurityPolicy policy = exchange.getAttribute(RouteSecurityPolicyFilter.POLICY_ATTR);
		if (!requiresJwt(policy)) {
			return chain.filter(exchange);
		}

		String method = exchange.getRequest().getMethod().name();
		if (!properties.getJwt().getNonce().getRequiredMethods().contains(method)) {
			return chain.filter(exchange);
		}

		GatewayPrincipal.User principal = exchange.getAttribute(GatewayPrincipal.ATTRIBUTE);
		if (principal == null) {
			return chain.filter(exchange);
		}

		String nonce = exchange.getRequest().getHeaders().getFirst("X-Nonce");
		if (nonce == null || nonce.isEmpty()) {
			return writeError(exchange, GatewayAuthErrorCode.NONCE_REQUIRED,
					"X-Nonce header is required for " + method + " requests");
		}

		return nonceValidator.validate(principal.sessionId(), nonce)
				.then(chain.filter(exchange))
				.onErrorResume(JwtAuthenticationException.class,
						e -> writeError(exchange, e.getErrorCode(), e.getMessage()));
	}

	@Override
	public int getOrder() {
		return GatewayFilterOrder.JWT_NONCE;
	}

	private boolean requiresJwt(RouteSecurityPolicy policy) {
		if (policy == null) {
			return false;
		}
		AuthMode mode = policy.getAuthMode();
		return mode == AuthMode.JWT || mode == AuthMode.JWT_OR_HMAC || mode == AuthMode.JWT_AND_HMAC;
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
