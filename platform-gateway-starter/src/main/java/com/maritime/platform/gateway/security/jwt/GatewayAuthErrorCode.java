package com.maritime.platform.gateway.security.jwt;

/**
 * Gateway authentication error codes used in error responses.
 */
public final class GatewayAuthErrorCode {

	private GatewayAuthErrorCode() {
	}

	public static final String MISSING_TOKEN = "MISSING_TOKEN";
	public static final String INVALID_TOKEN = "INVALID_TOKEN";
	public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
}
