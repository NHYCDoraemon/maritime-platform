package com.maritime.platform.gateway.security.hmac;

import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;

import org.springframework.data.redis.core.ReactiveRedisTemplate;

import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Default {@link AppCredentialResolver} that reads from a Redis hash
 * and falls back to configuration-based apps when Redis has no entry.
 *
 * <p>Redis key pattern: {@code {redisKeyPrefix}{appKey}} (default {@code iam:app:auth:{appKey}}).
 * Hash field names are configurable via {@code maritime.gateway.security.hmac.credentials.fields.*}.
 *
 * <p>Override by declaring a custom {@link AppCredentialResolver} bean.
 */
public class DefaultAppCredentialResolver implements AppCredentialResolver {

	private final GatewaySecurityProperties properties;
	private final ReactiveRedisTemplate<String, String> redisTemplate;

	public DefaultAppCredentialResolver(GatewaySecurityProperties properties,
			ReactiveRedisTemplate<String, String> redisTemplate) {
		this.properties = properties;
		this.redisTemplate = redisTemplate;
	}

	@Override
	public Mono<AppCredential> resolve(String appKey) {
		if (redisTemplate != null) {
			return resolveFromRedis(appKey)
					.switchIfEmpty(Mono.defer(() -> resolveFromConfig(appKey)));
		}
		return resolveFromConfig(appKey);
	}

	private Mono<AppCredential> resolveFromRedis(String appKey) {
		GatewaySecurityProperties.Credentials credentials = properties.getHmac().getCredentials();
		String redisKey = credentials.getRedisKeyPrefix() + appKey;

		return redisTemplate.<String, String>opsForHash()
				.entries(redisKey)
				.collectMap(Map.Entry::getKey, Map.Entry::getValue)
				.flatMap(entries -> {
					if (entries.isEmpty()) {
						return Mono.empty();
					}
					return mapToCredential(appKey, entries);
				});
	}

	private Mono<AppCredential> mapToCredential(String appKey, Map<String, String> entries) {
		GatewaySecurityProperties.CredentialFields fields = properties.getHmac().getCredentials().getFields();
		String enabledTrueValue = fields.getEnabledTrueValue();

		String enabledStr = entries.getOrDefault(fields.getEnabled(), enabledTrueValue);
		boolean enabled = enabledTrueValue.equalsIgnoreCase(enabledStr)
				|| "1".equals(enabledStr);

		if (!enabled) {
			return Mono.error(new JwtAuthenticationException(
					GatewayAuthErrorCode.APP_DISABLED,
					"App is disabled: " + appKey));
		}

		String permissionsStr = entries.getOrDefault(fields.getPermissions(), "");
		List<String> permissions = permissionsStr.isBlank()
				? List.of()
				: Arrays.asList(permissionsStr.split(","));

		AppCredential credential = AppCredential.builder()
				.appKey(appKey)
				.appSecret(entries.get(fields.getAppSecret()))
				.appCode(entries.get(fields.getAppCode()))
				.appId(entries.get(fields.getAppId()))
				.tenantId(entries.get(fields.getTenantId()))
				.tenantCode(entries.get(fields.getTenantCode()))
				.permissions(permissions)
				.enabled(enabled)
				.build();

		return Mono.just(credential);
	}

	private Mono<AppCredential> resolveFromConfig(String appKey) {
		for (GatewaySecurityProperties.ConfigApp app : properties.getHmac().getCredentials().getApps()) {
			if (app.getAppKey().equals(appKey)) {
				if (!app.isEnabled()) {
					return Mono.error(new JwtAuthenticationException(
							GatewayAuthErrorCode.APP_DISABLED,
							"App is disabled: " + appKey));
				}
				AppCredential credential = AppCredential.builder()
						.appKey(appKey)
						.appSecret(app.getAppSecret())
						.appCode(app.getAppCode())
						.enabled(true)
						.build();
				return Mono.just(credential);
			}
		}
		return Mono.error(new JwtAuthenticationException(
				GatewayAuthErrorCode.UNKNOWN_APP,
				"Unknown app key: " + appKey));
	}
}
