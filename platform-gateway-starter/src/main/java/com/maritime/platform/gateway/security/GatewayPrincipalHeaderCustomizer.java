package com.maritime.platform.gateway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Extension point for projects to add custom headers based on the
 * authenticated principal before the request is forwarded downstream.
 * <p>
 * Implementations are discovered as Spring beans and invoked in
 * {@link com.maritime.platform.gateway.filter.ContextHeaderInjectionFilter}
 * after the standard identity headers have been written.
 */
@FunctionalInterface
public interface GatewayPrincipalHeaderCustomizer {

	void customize(ServerHttpRequest.Builder builder, GatewayPrincipal principal);
}
