package com.maritime.platform.gateway.security.jwt;

import com.maritime.platform.gateway.security.GatewayPrincipal;
import io.jsonwebtoken.Claims;

/**
 * Extension point for mapping raw JWT claims to a {@link GatewayPrincipal.User}.
 * <p>
 * Implementations may enrich the principal from external sources;
 * the default implementation reads claim field names from
 * {@link com.maritime.platform.gateway.security.GatewaySecurityProperties.Claims}.
 */
@FunctionalInterface
public interface JwtClaimsMapper {

	GatewayPrincipal.User map(Claims claims);
}
