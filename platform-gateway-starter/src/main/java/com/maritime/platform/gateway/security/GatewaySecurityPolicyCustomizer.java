package com.maritime.platform.gateway.security;

import java.util.List;

/**
 * Extension point for projects to register additional security policies
 * programmatically. Implementations are discovered as Spring beans and
 * invoked during {@link RouteSecurityPolicyResolver} initialization.
 *
 * <p>Methods on the resolver ({@code addPublicPath}, {@code addRoutePolicy})
 * are only effective when called from a customizer, before pattern compilation.
 */
@FunctionalInterface
public interface GatewaySecurityPolicyCustomizer {

	void customize(RouteSecurityPolicyResolver resolver);
}
