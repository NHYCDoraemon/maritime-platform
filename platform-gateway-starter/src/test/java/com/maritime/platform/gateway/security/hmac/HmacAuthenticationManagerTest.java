package com.maritime.platform.gateway.security.hmac;

import com.maritime.platform.gateway.security.GatewayPrincipal;
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

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("HmacAuthenticationManager tests")
@ExtendWith(MockitoExtension.class)
class HmacAuthenticationManagerTest {

	private static final String APP_KEY = "app-001";
	private static final String APP_SECRET = "secret-key-for-testing";
	private static final String APP_CODE = "test-app";
	private static final Instant FIXED_TIME = Instant.ofEpochMilli(1700000000000L);

	@Mock
	private HmacNonceValidator nonceValidator;

	@Mock
	private AppCredentialResolver credentialResolver;

	private GatewaySecurityProperties properties;
	private HmacCanonicalRequestBuilder canonicalBuilder;
	private Clock clock;

	@BeforeEach
	void setUp() {
		properties = new GatewaySecurityProperties();
		properties.getHmac().setEnabled(true);

		canonicalBuilder = new HmacCanonicalRequestBuilder();
		clock = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
	}

	private HmacAuthenticationManager manager() {
		return new HmacAuthenticationManager(properties, canonicalBuilder,
				nonceValidator, credentialResolver, clock);
	}

	private AppCredential validCredential() {
		return AppCredential.builder()
				.appKey(APP_KEY)
				.appSecret(APP_SECRET)
				.appCode(APP_CODE)
				.enabled(true)
				.build();
	}

	/**
	 * Builds a valid HMAC signature for POST /api/data with the given inputs.
	 */
	private String sign(String timestamp, String nonce, byte[] bodyBytes) {
		return sign("POST", "/api/data", timestamp, nonce, bodyBytes);
	}

	private String sign(String method, String path, String timestamp, String nonce, byte[] bodyBytes) {
		String bodyDigest = HmacAuthenticationManager.sha256Hex(bodyBytes);
		String canonical = canonicalBuilder.build(APP_KEY, method, path,
				null, timestamp, nonce, bodyDigest);
		return HmacAuthenticationManager.hmacSha256Hex(APP_SECRET, canonical);
	}

	private String validTimestamp() {
		return String.valueOf(FIXED_TIME.toEpochMilli());
	}

	private void stubNonceOk(String appKey, String nonce) {
		when(nonceValidator.validate(eq(appKey), eq(nonce)))
				.thenReturn(Mono.empty());
	}

	private void stubCredentialOk() {
		when(credentialResolver.resolve(eq(APP_KEY)))
				.thenReturn(Mono.just(validCredential()));
	}

	// ---------- successful authentication ----------

	@Nested
	@DisplayName("Successful authentication")
	class SuccessfulAuth {

		@Test
		@DisplayName("valid signature returns App principal with correct fields")
		void validSignatureReturnsAppPrincipal() {
			String timestamp = validTimestamp();
			String nonce = "nonce-0123456789abcdef";
			byte[] body = "{\"data\":\"test\"}".getBytes(StandardCharsets.UTF_8);
			String bodyDigest = HmacAuthenticationManager.sha256Hex(body);
			String signature = sign(timestamp, nonce, body);

			stubNonceOk(APP_KEY, nonce);
			stubCredentialOk();

			StepVerifier.create(manager().authenticate(APP_KEY, timestamp, nonce,
							bodyDigest, signature, "POST", "/api/data", null, body))
					.consumeNextWith(principal -> {
						assertThat(principal).isNotNull();
						assertThat(principal.appKey()).isEqualTo(APP_KEY);
						assertThat(principal.appCode()).isEqualTo(APP_CODE);
					})
					.verifyComplete();
		}

		@Test
		@DisplayName("empty body digest matches SHA-256 of empty bytes")
		void emptyBodyAuthSucceeds() {
			String timestamp = validTimestamp();
			String nonce = "nonce-0123456789abcdef";
			byte[] body = new byte[0];
			String bodyDigest = HmacAuthenticationManager.sha256Hex(body);
			String signature = sign("GET", "/api/data", timestamp, nonce, body);

			stubNonceOk(APP_KEY, nonce);
			stubCredentialOk();

			StepVerifier.create(manager().authenticate(APP_KEY, timestamp, nonce,
							bodyDigest, signature, "GET", "/api/data", null, body))
					.expectNextCount(1)
					.verifyComplete();
		}

		@Test
		@DisplayName("nonce at minimum length is accepted")
		void minLengthNonceAccepted() {
			String timestamp = validTimestamp();
			String nonce = "0123456789abcdef"; // exactly 16 chars
			byte[] body = new byte[0];
			String bodyDigest = HmacAuthenticationManager.sha256Hex(body);
			String signature = sign("GET", "/api/data", timestamp, nonce, body);

			stubNonceOk(APP_KEY, nonce);
			stubCredentialOk();

			StepVerifier.create(manager().authenticate(APP_KEY, timestamp, nonce,
							bodyDigest, signature, "GET", "/api/data", null, body))
					.expectNextCount(1)
					.verifyComplete();
		}
	}

	// ---------- timestamp validation: millis vs seconds contract ----------

	@Test
	@DisplayName("epoch millis timestamp is accepted")
	void epochMillisTimestampAccepted() {
		// Current time in millis is the documented and implemented contract
		String timestamp = String.valueOf(System.currentTimeMillis());
		String nonce = "nonce-0123456789abcdef";
		byte[] body = new byte[0];
		String bodyDigest = HmacAuthenticationManager.sha256Hex(body);
		String signature = sign(timestamp, nonce, body);

		HmacAuthenticationManager realTimeManager = new HmacAuthenticationManager(
				properties, canonicalBuilder, nonceValidator, credentialResolver,
				Clock.systemUTC());

		stubNonceOk(APP_KEY, nonce);
		stubCredentialOk();

		StepVerifier.create(realTimeManager.authenticate(APP_KEY, timestamp, nonce,
						bodyDigest, signature, "POST", "/api/data", null, body))
				.expectNextCount(1)
				.verifyComplete();
	}

	@Test
	@DisplayName("epoch seconds timestamp is rejected with TIMESTAMP_EXPIRED")
	void epochSecondsTimestampRejected() {
		// epoch seconds (e.g. 1700000000) is ~50 years behind epoch millis,
		// so it falls far outside any reasonable tolerance window
		String timestampSeconds = String.valueOf(System.currentTimeMillis() / 1000);
		String nonce = "nonce-0123456789abcdef";
		byte[] body = new byte[0];
		String bodyDigest = HmacAuthenticationManager.sha256Hex(body);
		String signature = sign(timestampSeconds, nonce, body);

		HmacAuthenticationManager realTimeManager = new HmacAuthenticationManager(
				properties, canonicalBuilder, nonceValidator, credentialResolver,
				Clock.systemUTC());

		StepVerifier.create(realTimeManager.authenticate(APP_KEY, timestampSeconds, nonce,
						bodyDigest, signature, "POST", "/api/data", null, body))
				.verifyErrorSatisfies(e -> {
					assertThat(e)
							.isInstanceOf(JwtAuthenticationException.class)
							.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.TIMESTAMP_EXPIRED);
				});
	}

	@Nested
	@DisplayName("Timestamp validation")
	class TimestampValidation {

		@Test
		@DisplayName("future timestamp within tolerance is accepted")
		void futureTimestampWithinTolerance() {
			String timestamp = String.valueOf(FIXED_TIME.toEpochMilli() + Duration.ofMinutes(4).toMillis());
			String nonce = "nonce-0123456789abcdef";
			byte[] body = new byte[0];
			String bodyDigest = HmacAuthenticationManager.sha256Hex(body);
			String signature = sign(timestamp, nonce, body);

			stubNonceOk(APP_KEY, nonce);
			stubCredentialOk();

			StepVerifier.create(manager().authenticate(APP_KEY, timestamp, nonce,
							bodyDigest, signature, "POST", "/api/data", null, body))
					.expectNextCount(1)
					.verifyComplete();
		}

		@Test
		@DisplayName("timestamp beyond tolerance returns TIMESTAMP_EXPIRED")
		void timestampBeyondTolerance() {
			String timestamp = String.valueOf(FIXED_TIME.toEpochMilli() - Duration.ofMinutes(10).toMillis());
			String nonce = "nonce-0123456789abcdef";
			byte[] body = new byte[0];
			String bodyDigest = HmacAuthenticationManager.sha256Hex(body);
			String signature = sign(timestamp, nonce, body);

			StepVerifier.create(manager().authenticate(APP_KEY, timestamp, nonce,
							bodyDigest, signature, "POST", "/api/data", null, body))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.TIMESTAMP_EXPIRED);
					});
		}

		@Test
		@DisplayName("invalid timestamp format returns TIMESTAMP_EXPIRED")
		void invalidTimestampFormat() {
			StepVerifier.create(manager().authenticate(APP_KEY, "not-a-number",
							"nonce-0123456789abcdef", "digest", "sig",
							"POST", "/api/data", null, new byte[0]))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.TIMESTAMP_EXPIRED);
					});
		}
	}

	// ---------- nonce validation ----------

	@Nested
	@DisplayName("Nonce validation")
	class NonceValidation {

		@Test
		@DisplayName("nonce shorter than min length returns INVALID_SIGNATURE")
		void nonceTooShort() {
			String timestamp = validTimestamp();
			String nonce = "short";

			StepVerifier.create(manager().authenticate(APP_KEY, timestamp, nonce,
							"digest", "sig", "POST", "/api/data", null, new byte[0]))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_SIGNATURE);
					});
		}

		@Test
		@DisplayName("nonce replay returns REPLAY_DETECTED from Redis")
		void nonceReplayReturnsReplayDetected() {
			String timestamp = validTimestamp();
			String nonce = "nonce-0123456789abcdef";
			byte[] body = new byte[0];
			String bodyDigest = HmacAuthenticationManager.sha256Hex(body);
			String signature = sign(timestamp, nonce, body);

			when(nonceValidator.validate(eq(APP_KEY), eq(nonce)))
					.thenReturn(Mono.error(new JwtAuthenticationException(
							GatewayAuthErrorCode.REPLAY_DETECTED,
							"Nonce already used: " + nonce)));
			stubCredentialOk();

			StepVerifier.create(manager().authenticate(APP_KEY, timestamp, nonce,
							bodyDigest, signature, "POST", "/api/data", null, body))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.REPLAY_DETECTED);
					});
		}
	}

	// ---------- body digest validation ----------

	@Nested
	@DisplayName("Body digest validation")
	class BodyDigestValidation {

		@Test
		@DisplayName("mismatched body digest returns INVALID_SIGNATURE and never calls nonce validator")
		void mismatchedBodyDigestReturnsInvalidSignature() {
			String timestamp = validTimestamp();
			String nonce = "nonce-0123456789abcdef";
			byte[] body = "{\"data\":\"test\"}".getBytes(StandardCharsets.UTF_8);
			String wrongDigest = HmacAuthenticationManager.sha256Hex("different-body".getBytes(StandardCharsets.UTF_8));
			String signature = sign(timestamp, nonce, body);

			StepVerifier.create(manager().authenticate(APP_KEY, timestamp, nonce,
							wrongDigest, signature, "POST", "/api/data", null, body))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_SIGNATURE);
					});

			verify(nonceValidator, never()).validate(anyString(), anyString());
		}
	}

	// ---------- signature validation ----------

	@Nested
	@DisplayName("Signature validation")
	class SignatureValidation {

		@Test
		@DisplayName("wrong signature returns INVALID_SIGNATURE and never calls nonce validator")
		void wrongSignatureReturnsInvalidSignature() {
			String timestamp = validTimestamp();
			String nonce = "nonce-0123456789abcdef";
			byte[] body = new byte[0];
			String bodyDigest = HmacAuthenticationManager.sha256Hex(body);

			stubCredentialOk();

			StepVerifier.create(manager().authenticate(APP_KEY, timestamp, nonce,
							bodyDigest, "0000000000000000000000000000000000000000000000000000000000000000",
							"POST", "/api/data", null, body))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_SIGNATURE);
					});

			verify(nonceValidator, never()).validate(anyString(), anyString());
		}

		@Test
		@DisplayName("unknown app key returns UNKNOWN_APP from resolver and never calls nonce validator")
		void unknownAppKey() {
			String timestamp = validTimestamp();
			String nonce = "nonce-0123456789abcdef";
			byte[] body = new byte[0];
			String bodyDigest = HmacAuthenticationManager.sha256Hex(body);
			String signature = sign(timestamp, nonce, body);

			when(credentialResolver.resolve(eq("unknown-app")))
					.thenReturn(Mono.error(new JwtAuthenticationException(
							GatewayAuthErrorCode.UNKNOWN_APP,
							"Unknown app key: unknown-app")));

			StepVerifier.create(manager().authenticate("unknown-app", timestamp, nonce,
							bodyDigest, signature, "POST", "/api/data", null, body))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.UNKNOWN_APP);
						assertThat(e.getMessage()).contains("Unknown app key");
					});

			verify(nonceValidator, never()).validate(anyString(), anyString());
		}

		@Test
		@DisplayName("signature for different path does not match and never calls nonce validator")
		void signatureForDifferentPathFails() {
			String timestamp = validTimestamp();
			String nonce = "nonce-0123456789abcdef";
			byte[] body = new byte[0];
			String bodyDigest = HmacAuthenticationManager.sha256Hex(body);
			String signature = sign(timestamp, nonce, body);

			stubCredentialOk();

			StepVerifier.create(manager().authenticate(APP_KEY, timestamp, nonce,
							bodyDigest, signature, "POST", "/api/other", null, body))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_SIGNATURE);
					});

			verify(nonceValidator, never()).validate(anyString(), anyString());
		}

		@Test
		@DisplayName("constant-time comparison is used for signature verification")
		void constantTimeCompareUsed() {
			assertThat(HmacAuthenticationManager.constantTimeEquals("abc", "abc")).isTrue();
			assertThat(HmacAuthenticationManager.constantTimeEquals("abc", "abd")).isFalse();
			assertThat(HmacAuthenticationManager.constantTimeEquals(null, null)).isTrue();
			assertThat(HmacAuthenticationManager.constantTimeEquals("abc", null)).isFalse();
		}
	}

	// ---------- App credential resolution ----------

	@Nested
	@DisplayName("App credential resolution")
	class AppCredentialResolution {

		@Test
		@DisplayName("appCode falls back to appKey when credential has no code")
		void appCodeFallsBackToAppKey() {
			AppCredential noCodeCred = AppCredential.builder()
					.appKey("no-code-app")
					.appSecret("secret")
					.enabled(true)
					.build();

			String timestamp = validTimestamp();
			String nonce = "nonce-0123456789abcdef";
			byte[] body = new byte[0];
			String bodyDigest = HmacAuthenticationManager.sha256Hex(body);
			String canonical = canonicalBuilder.build("no-code-app", "POST", "/api/data",
					null, timestamp, nonce, bodyDigest);
			String signature = HmacAuthenticationManager.hmacSha256Hex("secret", canonical);

			when(nonceValidator.validate(eq("no-code-app"), eq(nonce)))
					.thenReturn(Mono.empty());
			when(credentialResolver.resolve(eq("no-code-app")))
					.thenReturn(Mono.just(noCodeCred));

			StepVerifier.create(manager().authenticate("no-code-app", timestamp, nonce,
							bodyDigest, signature, "POST", "/api/data", null, body))
					.consumeNextWith(p -> assertThat(p.appCode()).isEqualTo("no-code-app"))
					.verifyComplete();
		}

		@Test
		@DisplayName("disabled app returns APP_DISABLED from resolver and never calls nonce validator")
		void disabledAppReturnsAppDisabled() {
			String timestamp = validTimestamp();
			String nonce = "nonce-0123456789abcdef";
			byte[] body = new byte[0];
			String bodyDigest = HmacAuthenticationManager.sha256Hex(body);

			when(credentialResolver.resolve(eq("disabled-app")))
					.thenReturn(Mono.error(new JwtAuthenticationException(
							GatewayAuthErrorCode.APP_DISABLED,
							"App is disabled: disabled-app")));

			StepVerifier.create(manager().authenticate("disabled-app", timestamp, nonce,
							bodyDigest, "any-sig", "POST", "/api/data", null, body))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.APP_DISABLED);
					});

			verify(nonceValidator, never()).validate(anyString(), anyString());
		}

		@Test
		@DisplayName("missing appSecret returns INVALID_SIGNATURE and never calls nonce validator")
		void missingAppSecretReturnsInvalidSignature() {
			AppCredential noSecretCred = AppCredential.builder()
					.appKey("no-secret-app")
					.appCode("secretless")
					.enabled(true)
					.build();

			String timestamp = validTimestamp();
			String nonce = "nonce-0123456789abcdef";
			byte[] body = new byte[0];
			String bodyDigest = HmacAuthenticationManager.sha256Hex(body);

			when(credentialResolver.resolve(eq("no-secret-app")))
					.thenReturn(Mono.just(noSecretCred));

			StepVerifier.create(manager().authenticate("no-secret-app", timestamp, nonce,
							bodyDigest, "any-signature", "POST", "/api/data", null, body))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_SIGNATURE);
						assertThat(e.getMessage()).contains("App secret is missing");
					});

			verify(nonceValidator, never()).validate(anyString(), anyString());
		}
	}
}
