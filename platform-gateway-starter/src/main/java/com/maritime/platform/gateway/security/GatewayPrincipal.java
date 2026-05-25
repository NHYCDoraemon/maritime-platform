package com.maritime.platform.gateway.security;

import java.util.List;

/**
 * Authenticated principal placed in the exchange after successful authentication.
 */
public sealed interface GatewayPrincipal permits GatewayPrincipal.User, GatewayPrincipal.App {

	String ATTRIBUTE = "gateway.principal";

	record User(
			String userId,
			String userName,
			String sessionId,
			String activeOrgCode,
			String activeOrgName,
			List<String> systemScope,
			String userSource,
			String tenantId
	) implements GatewayPrincipal {

		public User {
			systemScope = systemScope != null ? List.copyOf(systemScope) : List.of();
		}
	}

	record App(
			String appKey,
			String appCode,
			String appId,
			String tenantId,
			String tenantCode,
			java.util.List<String> permissions
	) implements GatewayPrincipal {

		public App {
			permissions = permissions != null ? List.copyOf(permissions) : List.of();
		}
	}
}
