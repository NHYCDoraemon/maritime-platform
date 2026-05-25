package com.maritime.platform.gateway.security.jwt;

import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Validates JWT principal state against Redis: session existence,
 * token blacklist membership, and user enabled status.
 *
 * <p>Each check is independently toggled via
 * {@link GatewaySecurityProperties.Validation}. All checks are
 * reactive and non-blocking.
 */
@Component
@ConditionalOnProperty("maritime.gateway.security.jwt.enabled")
public class JwtStateValidator {

    private final ReactiveRedisOperations<String, String> redisOps;
    private final boolean requireSession;
    private final boolean checkBlacklist;
    private final boolean checkUserEnabled;
    private final String userEnabledDisabledValue;
    private final String sessionPrefix;
    private final String blacklistPrefix;
    private final String userEnabledPrefix;

    public JwtStateValidator(ReactiveRedisOperations<String, String> redisOps,
            GatewaySecurityProperties properties) {
        this.redisOps = redisOps;
        GatewaySecurityProperties.Validation validation = properties.getJwt().getValidation();
        GatewaySecurityProperties.RedisKeys redisKeys = properties.getJwt().getRedisKeys();
        this.requireSession = validation.isRequireSession();
        this.checkBlacklist = validation.isCheckBlacklist();
        this.checkUserEnabled = validation.isCheckUserEnabled();
        this.userEnabledDisabledValue = validation.getUserEnabledDisabledValue();
        this.sessionPrefix = redisKeys.getSessionPrefix();
        this.blacklistPrefix = redisKeys.getBlacklistPrefix();
        this.userEnabledPrefix = redisKeys.getUserEnabledPrefix();
    }

    /**
     * Validate session, blacklist, and user-enabled state against Redis.
     *
     * @param sessionId the session ID from JWT claims
     * @param jti       the JWT ID (jti claim), may be null
     * @param userId    the user ID from JWT claims
     * @return empty Mono on success, error Mono with {@link JwtAuthenticationException} on failure
     */
    public Mono<Void> validate(String sessionId, String jti, String userId) {
        return validateSession(sessionId)
                .then(Mono.defer(() -> validateBlacklist(jti)))
                .then(Mono.defer(() -> validateUserEnabled(userId)));
    }

    private Mono<Void> validateSession(String sessionId) {
        if (!requireSession) {
            return Mono.empty();
        }
        return redisOps.hasKey(sessionPrefix + sessionId)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.empty();
                    }
                    return Mono.error(new JwtAuthenticationException(
                            GatewayAuthErrorCode.SESSION_EXPIRED,
                            "Session expired or does not exist: " + sessionId));
                });
    }

    private Mono<Void> validateBlacklist(String jti) {
        if (!checkBlacklist || !StringUtils.hasText(jti)) {
            return Mono.empty();
        }
        return redisOps.hasKey(blacklistPrefix + jti)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new JwtAuthenticationException(
                                GatewayAuthErrorCode.TOKEN_BLACKLISTED,
                                "Token has been blacklisted: " + jti));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> validateUserEnabled(String userId) {
        if (!checkUserEnabled) {
            return Mono.empty();
        }
        return redisOps.opsForValue().get(userEnabledPrefix + userId)
                .flatMap(value -> {
                    if (userEnabledDisabledValue.equals(value)) {
                        return Mono.error(new JwtAuthenticationException(
                                GatewayAuthErrorCode.USER_DISABLED,
                                "User is disabled: " + userId));
                    }
                    return Mono.empty();
                })
                .then();
    }
}
