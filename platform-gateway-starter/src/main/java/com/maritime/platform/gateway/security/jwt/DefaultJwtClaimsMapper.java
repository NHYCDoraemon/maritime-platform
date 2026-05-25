package com.maritime.platform.gateway.security.jwt;

import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default {@link JwtClaimsMapper} that reads claim values using the
 * configurable field names from {@link GatewaySecurityProperties.Claims}.
 */
@Component
@ConditionalOnProperty("maritime.gateway.security.jwt.enabled")
public class DefaultJwtClaimsMapper implements JwtClaimsMapper {

	private final GatewaySecurityProperties.Claims claimsConfig;

	public DefaultJwtClaimsMapper(GatewaySecurityProperties props) {
		this.claimsConfig = props.getJwt().getClaims();
	}

	@Override
	public GatewayPrincipal.User map(Claims claims) {
		var config = claimsConfig;
		String userId = claims.get(config.getUserId(), String.class);
		String sessionId = claims.get(config.getSessionId(), String.class);
		String userName = claims.get(config.getUserName(), String.class);
		String activeOrgCode = claims.get(config.getActiveOrgCode(), String.class);
		String activeOrgName = claims.get(config.getActiveOrgName(), String.class);
		String userSource = claims.get(config.getUserSource(), String.class);
		String tenantId = claims.get(config.getTenantId(), String.class);

		List<String> systemScope = resolveSystemScope(claims);

		return new GatewayPrincipal.User(
				userId, userName, sessionId,
				activeOrgCode, activeOrgName,
				systemScope, userSource, tenantId
		);
	}

	private List<String> resolveSystemScope(Claims claims) {
		Object value = claims.get(claimsConfig.getSystemScope());
		if (value instanceof List<?> list) {
			return list.stream()
					.filter(obj -> obj instanceof String s && !s.isBlank())
					.map(obj -> (String) obj)
					.toList();
		}
		if (value instanceof String str && !str.isBlank()) {
			return List.of(str);
		}
		return List.of();
	}
}
