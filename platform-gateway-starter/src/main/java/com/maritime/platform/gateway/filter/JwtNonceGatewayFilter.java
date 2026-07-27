package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.error.GatewayErrorWriter;
import com.maritime.platform.gateway.security.AuthMode;
import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.RouteSecurityPolicy;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;
import com.maritime.platform.gateway.security.nonce.JwtNonceValidator;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Enforces JWT nonce validation for write requests on JWT-protected routes.
 * <p>
 * Runs after {@link JwtAuthenticationGatewayFilter} so the authenticated
 * principal is available. Only enforces nonce for HTTP methods configured
 * in {@code maritime.gateway.security.jwt.nonce.required-methods}
 * (default: POST, PUT, PATCH, DELETE).
 */
@Order(GatewayFilterOrder.JWT_NONCE)
public class JwtNonceGatewayFilter implements GlobalFilter, Ordered {

	private final JwtNonceValidator nonceValidator;
	private final GatewaySecurityProperties properties;
	private final GatewayErrorWriter errorWriter;

	public JwtNonceGatewayFilter(JwtNonceValidator nonceValidator,
			GatewaySecurityProperties properties, GatewayErrorWriter errorWriter) {
		this.nonceValidator = nonceValidator;
		this.properties = properties;
		this.errorWriter = errorWriter;
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
			return errorWriter.write(exchange, GatewayAuthErrorCode.NONCE_REQUIRED);
		}

		return nonceValidator.validate(principal.sessionId(), nonce)
				.then(chain.filter(exchange))
				.onErrorResume(JwtAuthenticationException.class,
						e -> errorWriter.write(exchange, e.getErrorCode()));
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
}
