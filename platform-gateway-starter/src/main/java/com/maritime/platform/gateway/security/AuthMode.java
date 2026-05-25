package com.maritime.platform.gateway.security;

/**
 * Route-level authentication mode.
 *
 * <p>{@link #JWT_AND_HMAC} is reserved for future high-security proxy scenarios
 * and is not treated as a completed capability in the current version.
 */
public enum AuthMode {

    NONE,
    JWT,
    HMAC,
    JWT_OR_HMAC,
    JWT_AND_HMAC

}
