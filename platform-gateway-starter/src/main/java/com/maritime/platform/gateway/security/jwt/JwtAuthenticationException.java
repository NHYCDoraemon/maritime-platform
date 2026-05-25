package com.maritime.platform.gateway.security.jwt;

/**
 * Thrown when JWT authentication fails, carrying a gateway error code.
 */
public class JwtAuthenticationException extends RuntimeException {

	private final String errorCode;

	public JwtAuthenticationException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}
}
