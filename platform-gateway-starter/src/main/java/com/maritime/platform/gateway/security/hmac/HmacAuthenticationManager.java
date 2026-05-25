package com.maritime.platform.gateway.security.hmac;

import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Authenticates system-to-system requests using HMAC-SHA256 signatures.
 *
 * <p>Validation sequence:
 * <ol>
 *   <li>Validate timestamp is within the configured tolerance window</li>
 *   <li>Validate nonce length meets the configured minimum</li>
 *   <li>Check nonce has not been replayed via Redis SETNX</li>
 *   <li>Compute SHA-256 of the request body and compare with client digest (constant-time)</li>
 *   <li>Build the canonical request string and compute HMAC-SHA256</li>
 *   <li>Compare computed signature with client signature (constant-time)</li>
 * </ol>
 */
@Component
@ConditionalOnProperty("maritime.gateway.security.hmac.enabled")
public class HmacAuthenticationManager {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final HexFormat HEX = HexFormat.of();

	private final GatewaySecurityProperties properties;
	private final HmacCanonicalRequestBuilder canonicalBuilder;
	private final HmacNonceValidator nonceValidator;
	private final Clock clock;

	public HmacAuthenticationManager(GatewaySecurityProperties properties,
			HmacCanonicalRequestBuilder canonicalBuilder,
			HmacNonceValidator nonceValidator) {
		this(properties, canonicalBuilder, nonceValidator, Clock.systemUTC());
	}

	public HmacAuthenticationManager(GatewaySecurityProperties properties,
			HmacCanonicalRequestBuilder canonicalBuilder,
			HmacNonceValidator nonceValidator, Clock clock) {
		this.properties = properties;
		this.canonicalBuilder = canonicalBuilder;
		this.nonceValidator = nonceValidator;
		this.clock = clock;
	}

	/**
	 * Authenticates an HMAC-signed request.
	 *
	 * @param appKey       the app key from X-App-Key header
	 * @param timestamp    epoch millis from X-Timestamp header
	 * @param nonce        nonce from X-Nonce header
	 * @param clientDigest SHA-256 hex of the request body from X-Body-Digest header
	 * @param clientSig    HMAC-SHA256 hex signature from X-Signature header
	 * @param method       uppercase HTTP method
	 * @param rawPath      raw URI path
	 * @param rawQuery     raw URI query string
	 * @param bodyBytes    the request body bytes (empty array for no body)
	 * @return Mono of App principal on success
	 */
	public Mono<GatewayPrincipal.App> authenticate(String appKey, String timestamp,
			String nonce, String clientDigest, String clientSig,
			String method, String rawPath, String rawQuery, byte[] bodyBytes) {

		GatewaySecurityProperties.Hmac hmac = properties.getHmac();
		Duration tolerance = hmac.getTimestampTolerance();
		int minNonceLength = hmac.getMinNonceLength();

		// 1. Validate timestamp window
		long ts;
		try {
			ts = Long.parseLong(timestamp);
		} catch (NumberFormatException e) {
			return Mono.error(new JwtAuthenticationException(
					GatewayAuthErrorCode.TIMESTAMP_EXPIRED,
					"Invalid timestamp format: " + timestamp));
		}
		long now = clock.millis();
		if (Math.abs(now - ts) > tolerance.toMillis()) {
			return Mono.error(new JwtAuthenticationException(
					GatewayAuthErrorCode.TIMESTAMP_EXPIRED,
					"Timestamp is outside the allowed tolerance window"));
		}

		// 2. Validate nonce length
		if (nonce == null || nonce.length() < minNonceLength) {
			return Mono.error(new JwtAuthenticationException(
					GatewayAuthErrorCode.INVALID_SIGNATURE,
					"Nonce is too short, minimum length is " + minNonceLength));
		}

		// 3. Compute body digest and compare
		String computedDigest = sha256Hex(bodyBytes);
		if (!constantTimeEquals(computedDigest, clientDigest)) {
			return Mono.error(new JwtAuthenticationException(
					GatewayAuthErrorCode.INVALID_SIGNATURE,
					"Body digest does not match"));
		}

		// 4. Validate nonce (Redis SETNX)
		return nonceValidator.validate(appKey, nonce)
				.then(Mono.fromCallable(() -> {
					// 5. Build canonical string and compute signature
					String canonical = canonicalBuilder.build(appKey, method, rawPath,
							rawQuery, timestamp, nonce, clientDigest);

					String appSecret = resolveAppSecret(appKey);
					if (appSecret == null) {
						throw new JwtAuthenticationException(
								GatewayAuthErrorCode.INVALID_SIGNATURE,
								"Unknown app key: " + appKey);
					}

					String computedSig = hmacSha256Hex(appSecret, canonical);
					if (!constantTimeEquals(computedSig, clientSig)) {
						throw new JwtAuthenticationException(
								GatewayAuthErrorCode.INVALID_SIGNATURE,
								"Signature does not match");
					}

					String appCode = resolveAppCode(appKey);
					return new GatewayPrincipal.App(appKey, appCode != null ? appCode : appKey);
				}));
	}

	private String resolveAppSecret(String appKey) {
		for (GatewaySecurityProperties.ConfigApp app : properties.getHmac().getCredentials().getApps()) {
			if (app.getAppKey().equals(appKey) && app.isEnabled()) {
				return app.getAppSecret();
			}
		}
		return null;
	}

	private String resolveAppCode(String appKey) {
		for (GatewaySecurityProperties.ConfigApp app : properties.getHmac().getCredentials().getApps()) {
			if (app.getAppKey().equals(appKey)) {
				return app.getAppCode();
			}
		}
		return null;
	}

	static String sha256Hex(byte[] data) {
		try {
			return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(data));
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 algorithm not available", e);
		}
	}

	static String hmacSha256Hex(String key, String data) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			SecretKeySpec spec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
			mac.init(spec);
			return HEX.formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
		} catch (java.security.NoSuchAlgorithmException | java.security.InvalidKeyException e) {
			throw new RuntimeException("HMAC-SHA256 algorithm not available", e);
		}
	}

	static boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null) {
			return a == b;
		}
		return MessageDigest.isEqual(
				a.getBytes(StandardCharsets.UTF_8),
				b.getBytes(StandardCharsets.UTF_8));
	}
}
