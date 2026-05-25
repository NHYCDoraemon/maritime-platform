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
	public static final String SESSION_EXPIRED = "SESSION_EXPIRED";
	public static final String TOKEN_BLACKLISTED = "TOKEN_BLACKLISTED";
	public static final String USER_DISABLED = "USER_DISABLED";
	public static final String NONCE_REQUIRED = "NONCE_REQUIRED";
	public static final String REPLAY_DETECTED = "REPLAY_DETECTED";
	public static final String MISSING_HMAC_HEADERS = "MISSING_HMAC_HEADERS";
	public static final String TIMESTAMP_EXPIRED = "TIMESTAMP_EXPIRED";
	public static final String INVALID_SIGNATURE = "INVALID_SIGNATURE";
}
