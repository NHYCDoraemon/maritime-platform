package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.security.GatewayPrincipal;

import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.List;

/**
 * Writes verified identity headers onto the downstream request.
 * <p>
 * JWT-authenticated users get user-id, org, session, scope, and
 * source headers. HMAC-authenticated apps get app-code, tenant,
 * and permissions headers. Both paths always set every defined
 * header so downstream services can trust their presence.
 */
public class TrustedHeaderWriter {

	static final String X_USER_ID = "X-User-Id";
	static final String X_USER_NAME = "X-User-Name";
	static final String X_ACTIVE_ORG_CODE = "X-Active-Org-Code";
	static final String X_ACTIVE_ORG_NAME = "X-Active-Org-Name";
	static final String X_TENANT_ID = "X-Tenant-Id";
	static final String X_SESSION_ID = "X-Session-Id";
	static final String X_SYSTEM_SCOPE = "X-System-Scope";
	static final String X_USER_SOURCE = "X-User-Source";

	static final String X_VERIFIED_APP_CODE = "X-Verified-App-Code";
	static final String X_APP_CODE = "X-App-Code";
	static final String X_APP_ID = "X-App-Id";
	static final String X_TENANT_CODE = "X-Tenant-Code";
	static final String X_APP_PERMISSIONS = "X-App-Permissions";

	public ServerHttpRequest.Builder writeUserHeaders(ServerHttpRequest.Builder builder,
			GatewayPrincipal.User user) {
		return builder
				.header(X_USER_ID, nullToEmpty(user.userId()))
				.header(X_USER_NAME, nullToEmpty(user.userName()))
				.header(X_ACTIVE_ORG_CODE, nullToEmpty(user.activeOrgCode()))
				.header(X_ACTIVE_ORG_NAME, nullToEmpty(user.activeOrgName()))
				.header(X_TENANT_ID, nullToEmpty(user.tenantId()))
				.header(X_SESSION_ID, nullToEmpty(user.sessionId()))
				.header(X_SYSTEM_SCOPE, listToHeader(user.systemScope()))
				.header(X_USER_SOURCE, nullToEmpty(user.userSource()));
	}

	public ServerHttpRequest.Builder writeAppHeaders(ServerHttpRequest.Builder builder,
			GatewayPrincipal.App app) {
		return builder
				.header(X_VERIFIED_APP_CODE, nullToEmpty(app.appCode()))
				.header(X_APP_CODE, nullToEmpty(app.appCode()))
				.header(X_APP_ID, nullToEmpty(app.appId()))
				.header(X_TENANT_CODE, nullToEmpty(app.tenantCode()))
				.header(X_TENANT_ID, nullToEmpty(app.tenantId()))
				.header(X_APP_PERMISSIONS, listToHeader(app.permissions()));
	}

	private static String nullToEmpty(String s) {
		return s != null ? s : "";
	}

	private static String listToHeader(List<String> list) {
		return list != null && !list.isEmpty() ? String.join(",", list) : "";
	}
}
