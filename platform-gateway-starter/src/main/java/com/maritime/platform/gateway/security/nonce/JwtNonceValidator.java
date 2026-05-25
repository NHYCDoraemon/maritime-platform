package com.maritime.platform.gateway.security.nonce;

import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Validates JWT request nonces using Redis SETNX with TTL to prevent
 * replay attacks on write operations.
 *
 * <p>The atomic {@code SET key value NX EX ttl} ensures that even under
 * concurrent requests, only the first write succeeds and subsequent
 * attempts are correctly detected as replays.
 */
@Component
@ConditionalOnProperty("maritime.gateway.security.jwt.enabled")
public class JwtNonceValidator {

	private final ReactiveRedisOperations<String, String> redisOps;
	private final String keyPrefix;
	private final Duration ttl;

	public JwtNonceValidator(ReactiveRedisOperations<String, String> redisOps,
			GatewaySecurityProperties properties) {
		this.redisOps = redisOps;
		GatewaySecurityProperties.JwtNonce nonce = properties.getJwt().getNonce();
		this.keyPrefix = nonce.getSimpleKeyPrefix();
		this.ttl = nonce.getTtl();
	}

	/**
	 * Validate that the nonce has not been used before for this session.
	 * Uses atomic Redis SETNX with TTL.
	 *
	 * @param sessionId the session ID from the authenticated principal
	 * @param nonce     the nonce value from the X-Nonce header
	 * @return empty Mono on success, error with REPLAY_DETECTED if nonce was already used
	 */
	public Mono<Void> validate(String sessionId, String nonce) {
		String key = keyPrefix + sessionId + ":" + nonce;
		return redisOps.opsForValue().setIfAbsent(key, "1", ttl)
				.flatMap(accepted -> {
					if (Boolean.TRUE.equals(accepted)) {
						return Mono.empty();
					}
					return Mono.error(new JwtAuthenticationException(
							GatewayAuthErrorCode.REPLAY_DETECTED,
							"Nonce has already been used: " + nonce));
				});
	}
}
