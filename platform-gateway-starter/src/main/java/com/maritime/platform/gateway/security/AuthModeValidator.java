package com.maritime.platform.gateway.security;

/**
 * Validates that an {@link AuthMode} is consistent with the enabled state
 * of JWT and HMAC authentication components.
 *
 * <p>Shared by {@link GatewaySecurityProperties} (for configuration-driven
 * routes) and {@link RouteSecurityPolicyResolver} (for programmatic routes).
 */
final class AuthModeValidator {

    private AuthModeValidator() {}

    /**
     * Validates that the given auth mode is supported by the current
     * enabled flags.
     *
     * @param mode        the auth mode to validate
     * @param source      human-readable source description for error messages
     * @param jwtEnabled  whether JWT authentication is enabled
     * @param hmacEnabled whether HMAC authentication is enabled
     * @throws IllegalArgumentException if the mode requires a disabled component
     */
    static void validateEnabled(AuthMode mode, String source, boolean jwtEnabled, boolean hmacEnabled) {
        switch (mode) {
            case JWT:
                if (!jwtEnabled) {
                    throw new IllegalArgumentException(
                            source + " is JWT but maritime.gateway.security.jwt.enabled is not true");
                }
                break;
            case HMAC:
                if (!hmacEnabled) {
                    throw new IllegalArgumentException(
                            source + " is HMAC but maritime.gateway.security.hmac.enabled is not true");
                }
                break;
            case JWT_OR_HMAC:
                if (!jwtEnabled || !hmacEnabled) {
                    throw new IllegalArgumentException(
                            source + " is JWT_OR_HMAC but requires both "
                            + "maritime.gateway.security.jwt.enabled=true and "
                            + "maritime.gateway.security.hmac.enabled=true");
                }
                break;
            case NONE:
            case JWT_AND_HMAC:
                break;
        }
    }
}
