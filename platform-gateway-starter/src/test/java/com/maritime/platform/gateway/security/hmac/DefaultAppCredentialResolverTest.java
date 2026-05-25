package com.maritime.platform.gateway.security.hmac;

import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("DefaultAppCredentialResolver tests")
@ExtendWith(MockitoExtension.class)
class DefaultAppCredentialResolverTest {

	private static final String APP_KEY = "app-001";
	private static final String APP_SECRET = "redis-secret";
	private static final String REDIS_KEY = "iam:app:auth:app-001";

	@Mock
	private ReactiveRedisTemplate<String, String> redisTemplate;

	@Mock
	private ReactiveHashOperations<String, String, String> hashOps;

	private GatewaySecurityProperties properties;

	@BeforeEach
	void setUp() {
		properties = new GatewaySecurityProperties();
		properties.getHmac().setEnabled(true);
	}

	private DefaultAppCredentialResolver resolver() {
		return new DefaultAppCredentialResolver(properties, redisTemplate);
	}

	private DefaultAppCredentialResolver resolverWithoutRedis() {
		return new DefaultAppCredentialResolver(properties, null);
	}

	private Flux<Map.Entry<String, String>> redisEntryFlux(Map<String, String> map) {
		return Flux.fromIterable(map.entrySet());
	}

	private Map<String, String> redisEntry(String appSecret) {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("appSecret", appSecret);
		map.put("appCode", "test-app");
		map.put("appId", "app-id-123");
		map.put("tenantId", "tenant-1");
		map.put("tenantCode", "T001");
		map.put("permissions", "read,write");
		map.put("isEnabled", "true");
		return map;
	}

	private void stubRedisEntries(Map<String, String> entries) {
		when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
		when(hashOps.entries(eq(REDIS_KEY))).thenReturn(redisEntryFlux(entries));
	}

	// ---------- Redis hit ----------

	@Nested
	@DisplayName("Redis resolution")
	class RedisResolution {

		@Test
		@DisplayName("returns credential when Redis hash has all fields")
		void returnsCredentialFromRedis() {
			stubRedisEntries(redisEntry(APP_SECRET));

			StepVerifier.create(resolver().resolve(APP_KEY))
					.consumeNextWith(cred -> {
						assertThat(cred.getAppKey()).isEqualTo(APP_KEY);
						assertThat(cred.getAppSecret()).isEqualTo(APP_SECRET);
						assertThat(cred.getAppCode()).isEqualTo("test-app");
						assertThat(cred.getAppId()).isEqualTo("app-id-123");
						assertThat(cred.getTenantId()).isEqualTo("tenant-1");
						assertThat(cred.getTenantCode()).isEqualTo("T001");
						assertThat(cred.getPermissions()).containsExactly("read", "write");
						assertThat(cred.isEnabled()).isTrue();
					})
					.verifyComplete();
		}

		@Test
		@DisplayName("treats enabled field value '1' as true")
		void enabledFieldOneIsTrue() {
			Map<String, String> entry = redisEntry(APP_SECRET);
			entry.put("isEnabled", "1");
			stubRedisEntries(entry);

			StepVerifier.create(resolver().resolve(APP_KEY))
					.consumeNextWith(cred -> assertThat(cred.isEnabled()).isTrue())
					.verifyComplete();
		}

		@Test
		@DisplayName("returns APP_DISABLED when Redis has isEnabled=false")
		void disabledAppReturnsAppDisabled() {
			Map<String, String> entry = redisEntry(APP_SECRET);
			entry.put("isEnabled", "false");
			stubRedisEntries(entry);

			StepVerifier.create(resolver().resolve(APP_KEY))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.APP_DISABLED);
						assertThat(e.getMessage()).contains("disabled");
					});
		}

		@Test
		@DisplayName("treats missing enabled field as enabled (default true)")
		void missingEnabledFieldDefaultsToTrue() {
			Map<String, String> entry = new LinkedHashMap<>();
			entry.put("appSecret", APP_SECRET);
			entry.put("appCode", "test-app");
			stubRedisEntries(entry);

			StepVerifier.create(resolver().resolve(APP_KEY))
					.consumeNextWith(cred -> assertThat(cred.isEnabled()).isTrue())
					.verifyComplete();
		}

		@Test
		@DisplayName("empty permissions field produces empty list")
		void emptyPermissionsProducesEmptyList() {
			Map<String, String> entry = redisEntry(APP_SECRET);
			entry.put("permissions", "");
			stubRedisEntries(entry);

			StepVerifier.create(resolver().resolve(APP_KEY))
					.consumeNextWith(cred -> assertThat(cred.getPermissions()).isEmpty())
					.verifyComplete();
		}
	}

	// ---------- Redis miss → config fallback ----------

	@Nested
	@DisplayName("Config fallback")
	class ConfigFallback {

		@Test
		@DisplayName("falls back to config app when Redis returns empty hash")
		void fallsBackToConfigWhenRedisEmpty() {
			GatewaySecurityProperties.ConfigApp app = new GatewaySecurityProperties.ConfigApp();
			app.setAppKey(APP_KEY);
			app.setAppSecret("config-secret");
			app.setAppCode("config-app");
			app.setEnabled(true);
			properties.getHmac().getCredentials().getApps().add(app);

			when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
			when(hashOps.entries(eq(REDIS_KEY))).thenReturn(Flux.empty());

			StepVerifier.create(resolver().resolve(APP_KEY))
					.consumeNextWith(cred -> {
						assertThat(cred.getAppSecret()).isEqualTo("config-secret");
						assertThat(cred.getAppCode()).isEqualTo("config-app");
						assertThat(cred.isEnabled()).isTrue();
					})
					.verifyComplete();
		}

		@Test
		@DisplayName("returns UNKNOWN_APP when Redis misses and no config match")
		void unknownAppWhenNoRedisAndNoConfig() {
			when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
			when(hashOps.entries(eq(REDIS_KEY))).thenReturn(Flux.empty());

			StepVerifier.create(resolver().resolve(APP_KEY))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.UNKNOWN_APP);
						assertThat(e.getMessage()).contains("Unknown app key");
					});
		}

		@Test
		@DisplayName("returns APP_DISABLED when config app is disabled")
		void disabledConfigAppReturnsAppDisabled() {
			GatewaySecurityProperties.ConfigApp app = new GatewaySecurityProperties.ConfigApp();
			app.setAppKey(APP_KEY);
			app.setAppSecret("config-secret");
			app.setAppCode("config-app");
			app.setEnabled(false);
			properties.getHmac().getCredentials().getApps().add(app);

			when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
			when(hashOps.entries(eq(REDIS_KEY))).thenReturn(Flux.empty());

			StepVerifier.create(resolver().resolve(APP_KEY))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.APP_DISABLED);
					});
		}
	}

	// ---------- No Redis ----------

	@Nested
	@DisplayName("Without Redis")
	class WithoutRedis {

		@Test
		@DisplayName("resolves from config directly when Redis is unavailable")
		void resolvesFromConfigWhenNoRedis() {
			GatewaySecurityProperties.ConfigApp app = new GatewaySecurityProperties.ConfigApp();
			app.setAppKey(APP_KEY);
			app.setAppSecret("config-secret");
			app.setAppCode("config-app");
			app.setEnabled(true);
			properties.getHmac().getCredentials().getApps().add(app);

			StepVerifier.create(resolverWithoutRedis().resolve(APP_KEY))
					.consumeNextWith(cred -> {
						assertThat(cred.getAppSecret()).isEqualTo("config-secret");
						assertThat(cred.getAppCode()).isEqualTo("config-app");
					})
					.verifyComplete();
		}

		@Test
		@DisplayName("returns UNKNOWN_APP when no Redis and no config match")
		void unknownAppWhenNoRedisAndNoConfigMatch() {
			StepVerifier.create(resolverWithoutRedis().resolve("unknown-app"))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.UNKNOWN_APP);
					});
		}
	}

	// ---------- Custom field names ----------

	@Nested
	@DisplayName("Custom field mappings")
	class CustomFieldMappings {

		@Test
		@DisplayName("uses configured field names for Redis hash mapping")
		void usesConfiguredFieldNames() {
			properties.getHmac().getCredentials().getFields().setAppSecret("secret");
			properties.getHmac().getCredentials().getFields().setAppCode("code");
			properties.getHmac().getCredentials().getFields().setEnabled("active");
			properties.getHmac().getCredentials().getFields().setEnabledTrueValue("yes");

			Map<String, String> entry = new LinkedHashMap<>();
			entry.put("secret", "custom-secret");
			entry.put("code", "custom-code");
			entry.put("active", "yes");

			when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
			when(hashOps.entries(eq(REDIS_KEY))).thenReturn(redisEntryFlux(entry));

			StepVerifier.create(resolver().resolve(APP_KEY))
					.consumeNextWith(cred -> {
						assertThat(cred.getAppSecret()).isEqualTo("custom-secret");
						assertThat(cred.getAppCode()).isEqualTo("custom-code");
						assertThat(cred.isEnabled()).isTrue();
					})
					.verifyComplete();
		}
	}
}
