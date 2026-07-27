package com.maritime.platform.gateway.security.hmac;

import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;

import org.springframework.data.redis.core.ReactiveRedisOperations;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Validates HMAC request nonces using Redis SETNX with TTL to prevent
 * replay attacks on system-to-system requests.
 *
 * <p>Key format: {@code {nonceKeyPrefix}{appKey}:{nonce}}
 */
public class HmacNonceValidator {

	private final ReactiveRedisOperations<String, String> redisOps;
	private final String keyPrefix;
	private final Duration ttl;

	public HmacNonceValidator(ReactiveRedisOperations<String, String> redisOps,
			GatewaySecurityProperties properties) {
		this.redisOps = redisOps;
		GatewaySecurityProperties.Hmac hmac = properties.getHmac();
		this.keyPrefix = hmac.getNonceKeyPrefix();
		this.ttl = hmac.getNonceTtl();
	}

	/**
	 * Validate that the nonce has not been used before for this app.
	 *
	 * @param appKey the app key from X-App-Key header
	 * @param nonce  the nonce value from X-Nonce header
	 * @return empty Mono on success, error with REPLAY_DETECTED if nonce was already used
	 */
	public Mono<Void> validate(String appKey, String nonce) {
		String key = keyPrefix + appKey + ":" + nonce;
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
