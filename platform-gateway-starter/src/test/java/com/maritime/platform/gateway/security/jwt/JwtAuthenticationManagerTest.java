package com.maritime.platform.gateway.security.jwt;

import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("JwtAuthenticationManager tests")
class JwtAuthenticationManagerTest {

	private static final String SECRET = "my-test-secret-key-minimum-256-bits-long!!";
	private static final String ISSUER = "maritime-platform";

	private GatewaySecurityProperties properties;
	private SecretKey signingKey;

	@BeforeEach
	void setUp() {
		properties = new GatewaySecurityProperties();
		properties.getJwt().setEnabled(true);
		properties.getJwt().setSecret(SECRET);
		properties.getJwt().setIssuer(ISSUER);

		signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
	}

	private JwtAuthenticationManager manager() {
		return manager(Clock.systemUTC());
	}

	private JwtAuthenticationManager manager(Clock clock) {
		properties.getJwt().setEnabled(true);
		properties.getJwt().setSecret(SECRET);
		properties.getJwt().setIssuer(ISSUER);
		return new JwtAuthenticationManager(properties, claimsMapper(), clock);
	}

	private JwtClaimsMapper claimsMapper() {
		return new DefaultJwtClaimsMapper(properties);
	}

	private String createToken(Instant issuedAt, Instant expiration, String userId, String sessionId) {
		return Jwts.builder()
				.issuer(ISSUER)
				.subject(userId)
				.claim("userId", userId)
				.claim("userName", "TestUser")
				.claim("sessionId", sessionId)
				.claim("activeOrgCode", "ORG001")
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiration))
				.signWith(signingKey)
				.compact();
	}

	private String createToken(Instant issuedAt, Instant expiration) {
		return createToken(issuedAt, expiration, "user-123", "session-abc");
	}

	// ---------- successful authentication ----------

	@Nested
	@DisplayName("Successful authentication")
	class SuccessfulAuth {

		@Test
		@DisplayName("returns user principal for valid token")
		void returnsPrincipalForValidToken() {
			Instant now = Instant.now();
			String token = createToken(now, now.plus(1, ChronoUnit.HOURS));

			StepVerifier.create(manager().authenticate(token))
					.assertNext(user -> {
						assertThat(user.userId()).isEqualTo("user-123");
						assertThat(user.userName()).isEqualTo("TestUser");
						assertThat(user.sessionId()).isEqualTo("session-abc");
						assertThat(user.activeOrgCode()).isEqualTo("ORG001");
					})
					.verifyComplete();
		}

		@Test
		@DisplayName("accepts token within clock skew window")
		void acceptsTokenWithinClockSkew() {
			Instant now = Instant.now();
			Instant expiry = now.minus(10, ChronoUnit.SECONDS); // 10s ago
			String token = createToken(now.minus(1, ChronoUnit.HOURS), expiry);

			// Default clock skew is 30s, so 10s past expiry is fine
			Clock fixedClock = Clock.fixed(now, ZoneId.systemDefault());
			StepVerifier.create(manager(fixedClock).authenticate(token))
					.assertNext(user -> assertThat(user.userId()).isEqualTo("user-123"))
					.verifyComplete();
		}

		@Test
		@DisplayName("maps systemScope as list of strings")
		void mapsSystemScopeList() {
			Instant now = Instant.now();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.claim("userId", "user-123")
					.claim("sessionId", "session-abc")
					.claim("systemScope", List.of("scope-a", "scope-b"))
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			StepVerifier.create(manager().authenticate(token))
					.assertNext(user -> {
						assertThat(user.systemScope()).containsExactly("scope-a", "scope-b");
					})
					.verifyComplete();
		}

		@Test
		@DisplayName("maps systemScope from single string value")
		void mapsSystemScopeFromString() {
			Instant now = Instant.now();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.claim("userId", "user-123")
					.claim("sessionId", "session-abc")
					.claim("systemScope", "single-scope")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			StepVerifier.create(manager().authenticate(token))
					.assertNext(user -> {
						assertThat(user.systemScope()).containsExactly("single-scope");
					})
					.verifyComplete();
		}
	}

	// ---------- missing token ----------

	@Nested
	@DisplayName("Missing token")
	class MissingToken {

		@Test
		@DisplayName("null token produces INVALID_TOKEN")
		void nullTokenIsInvalid() {
			StepVerifier.create(manager().authenticate(null))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_TOKEN);
					});
		}

		@Test
		@DisplayName("empty token produces INVALID_TOKEN")
		void emptyTokenIsInvalid() {
			StepVerifier.create(manager().authenticate(""))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_TOKEN);
					});
		}

		@Test
		@DisplayName("malformed token produces INVALID_TOKEN")
		void malformedTokenIsInvalid() {
			StepVerifier.create(manager().authenticate("not-a-valid-jwt"))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_TOKEN);
					});
		}
	}

	// ---------- invalid token ----------

	@Nested
	@DisplayName("Invalid token")
	class InvalidToken {

		@Test
		@DisplayName("token signed with wrong key produces INVALID_TOKEN")
		void wrongKeyIsInvalid() {
			SecretKey otherKey = Keys.hmacShaKeyFor("another-secret-key-that-is-different!!".getBytes(StandardCharsets.UTF_8));
			Instant now = Instant.now();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.claim("userId", "user-123")
					.claim("sessionId", "session-abc")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(otherKey)
					.compact();

			StepVerifier.create(manager().authenticate(token))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_TOKEN);
					});
		}

		@Test
		@DisplayName("token with wrong issuer produces INVALID_TOKEN")
		void wrongIssuerIsInvalid() {
			Instant now = Instant.now();
			String token = Jwts.builder()
					.issuer("wrong-issuer")
					.claim("userId", "user-123")
					.claim("sessionId", "session-abc")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			StepVerifier.create(manager().authenticate(token))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_TOKEN);
					});
		}

		@Test
		@DisplayName("tampered token payload produces INVALID_TOKEN")
		void tamperedTokenIsInvalid() {
			Instant now = Instant.now();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.claim("userId", "user-123")
					.claim("sessionId", "session-abc")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			// Tamper with the token by changing the payload part
			String[] parts = token.split("\\.");
			String tamperedPayload = "dGFtcGVyZWQ"; // base64 of "tampered"
			String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

			StepVerifier.create(manager().authenticate(tamperedToken))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_TOKEN);
					});
		}
	}

	// ---------- expired token ----------

	@Nested
	@DisplayName("Expired token")
	class ExpiredToken {

		@Test
		@DisplayName("expired token beyond clock skew produces TOKEN_EXPIRED")
		void expiredBeyondSkewIsExpired() {
			Instant now = Instant.now();
			Instant issuedAt = now.minus(2, ChronoUnit.HOURS);
			Instant expiry = now.minus(1, ChronoUnit.HOURS); // 1 hour ago
			String token = createToken(issuedAt, expiry);

			StepVerifier.create(manager().authenticate(token))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.TOKEN_EXPIRED);
					});
		}

		@Test
		@DisplayName("token just expired within clock skew is accepted")
		void justExpiredWithinSkewIsAccepted() {
			Instant now = Instant.now();
			Instant issuedAt = now.minus(1, ChronoUnit.HOURS);
			Instant expiry = now.minus(20, ChronoUnit.SECONDS); // 20s ago (< 30s skew)
			String token = createToken(issuedAt, expiry);

			StepVerifier.create(manager().authenticate(token))
					.assertNext(user -> assertThat(user.userId()).isEqualTo("user-123"))
					.verifyComplete();
		}

		@Test
		@DisplayName("token expired within clock skew window is accepted")
		void withinClockSkewIsAccepted() {
			Instant now = Instant.now();
			Instant issuedAt = now.minus(1, ChronoUnit.HOURS);
			Instant expiry = now.minusSeconds(5); // 5s ago, well within 30s skew
			String token = createToken(issuedAt, expiry);

			Clock fixedClock = Clock.fixed(now, ZoneId.systemDefault());
			StepVerifier.create(manager(fixedClock).authenticate(token))
					.assertNext(user -> assertThat(user.userId()).isEqualTo("user-123"))
					.verifyComplete();
		}

		@Test
		@DisplayName("expired token with zero clock skew produces TOKEN_EXPIRED immediately")
		void zeroClockSkewExpired() {
			properties.getJwt().setClockSkewSeconds(0);
			Instant now = Instant.now();
			Instant issuedAt = now.minus(1, ChronoUnit.HOURS);
			Instant expiry = now.minus(1, ChronoUnit.SECONDS); // 1s ago
			String token = createToken(issuedAt, expiry);

			StepVerifier.create(manager().authenticate(token))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.TOKEN_EXPIRED);
					});
		}
	}

	// ---------- missing claims ----------

	@Nested
	@DisplayName("Missing required claims")
	class MissingClaims {

		@Test
		@DisplayName("missing userId claim produces INVALID_TOKEN")
		void missingUserIdIsInvalid() {
			Instant now = Instant.now();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.claim("sessionId", "session-abc")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			StepVerifier.create(manager().authenticate(token))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_TOKEN);
						assertThat(e).hasMessageContaining("userId");
					});
		}

		@Test
		@DisplayName("missing sessionId claim produces INVALID_TOKEN")
		void missingSessionIdIsInvalid() {
			Instant now = Instant.now();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.claim("userId", "user-123")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			StepVerifier.create(manager().authenticate(token))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_TOKEN);
						assertThat(e).hasMessageContaining("sessionId");
					});
		}

		@Test
		@DisplayName("empty userId string produces INVALID_TOKEN")
		void emptyUserIdIsInvalid() {
			Instant now = Instant.now();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.claim("userId", "")
					.claim("sessionId", "session-abc")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			StepVerifier.create(manager().authenticate(token))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_TOKEN);
					});
		}

		@Test
		@DisplayName("empty sessionId string produces INVALID_TOKEN")
		void emptySessionIdIsInvalid() {
			Instant now = Instant.now();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.claim("userId", "user-123")
					.claim("sessionId", "")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			StepVerifier.create(manager().authenticate(token))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_TOKEN);
					});
		}
	}

	// ---------- encrypted JWT ----------

	@Nested
	@DisplayName("Encrypted JWT")
	class EncryptedJwt {

		@Test
		@DisplayName("decrypts encrypted token and authenticates successfully")
		void decryptsAndAuthenticates() {
			properties.getJwt().setEncrypted(true);
			Instant now = Instant.now();
			String rawToken = createToken(now, now.plus(1, ChronoUnit.HOURS));
			JwtEncryptor encryptor = new JwtEncryptor(SECRET);
			String encryptedToken = encryptor.encrypt(rawToken);

			StepVerifier.create(manager().authenticate(encryptedToken))
					.assertNext(user -> {
						assertThat(user.userId()).isEqualTo("user-123");
						assertThat(user.sessionId()).isEqualTo("session-abc");
					})
					.verifyComplete();
		}

		@Test
		@DisplayName("garbage encrypted blob produces INVALID_TOKEN")
		void garbageEncryptedIsInvalid() {
			properties.getJwt().setEncrypted(true);

			StepVerifier.create(manager().authenticate("garbage-encrypted-blob"))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.INVALID_TOKEN);
					});
		}
	}

	// ---------- custom claims mapping ----------

	@Nested
	@DisplayName("Custom claims field names")
	class CustomClaimsMapping {

		@Test
		@DisplayName("respects custom claim field name configuration")
		void usesCustomClaimNames() {
			properties.getJwt().getClaims().setUserId("uid");
			properties.getJwt().getClaims().setSessionId("sid");
			properties.getJwt().getClaims().setUserName("uname");

			Instant now = Instant.now();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.claim("uid", "custom-user-id")
					.claim("uname", "CustomName")
					.claim("sid", "custom-session-id")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			StepVerifier.create(manager().authenticate(token))
					.assertNext(user -> {
						assertThat(user.userId()).isEqualTo("custom-user-id");
						assertThat(user.userName()).isEqualTo("CustomName");
						assertThat(user.sessionId()).isEqualTo("custom-session-id");
					})
					.verifyComplete();
		}
	}

	// ---------- principal immutability ----------

	@Nested
	@DisplayName("Principal immutability")
	class PrincipalImmutability {

		@Test
		@DisplayName("systemScope list is defensively copied")
		void systemScopeIsDefensivelyCopied() {
			Instant now = Instant.now();
			String token = createToken(now, now.plus(1, ChronoUnit.HOURS));

			GatewayPrincipal.User user = manager().authenticate(token).block();
			assertThat(user.systemScope()).isNotNull();

			// Attempting to modify should throw
			assertThat((Object) user.systemScope()).isInstanceOf(Object.class);
			assertThatThrownBy(() -> user.systemScope().add("extra"))
					.isInstanceOf(UnsupportedOperationException.class);
		}

		// Dummy import-to-declare for assertThatThrownBy
	}

	// ---------- state validation integration ----------

	@Nested
	@DisplayName("State validation integration")
	class StateValidationIntegration {

		@Test
		@DisplayName("passes when state validator approves")
		void passesWhenStateValidatorApproves() {
			Instant now = Instant.now();
			String jti = UUID.randomUUID().toString();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.id(jti)
					.claim("userId", "user-123")
					.claim("sessionId", "session-abc")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			JwtStateValidator stateValidator = Mockito.mock(JwtStateValidator.class);
			when(stateValidator.validate(any(), any(), any())).thenReturn(Mono.empty());

			JwtAuthenticationManager mgr = new JwtAuthenticationManager(
					properties, claimsMapper(), Clock.systemUTC(), stateValidator);

			StepVerifier.create(mgr.authenticate(token))
					.assertNext(user -> assertThat(user.userId()).isEqualTo("user-123"))
					.verifyComplete();
		}

		@Test
		@DisplayName("fails with SESSION_EXPIRED when session check fails")
		void failsWithSessionExpiredWhenSessionCheckFails() {
			Instant now = Instant.now();
			String jti = UUID.randomUUID().toString();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.id(jti)
					.claim("userId", "user-123")
					.claim("sessionId", "session-abc")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			JwtStateValidator stateValidator = Mockito.mock(JwtStateValidator.class);
			when(stateValidator.validate(any(), any(), any()))
					.thenReturn(Mono.error(new JwtAuthenticationException(
							GatewayAuthErrorCode.SESSION_EXPIRED, "Session expired")));

			JwtAuthenticationManager mgr = new JwtAuthenticationManager(
					properties, claimsMapper(), Clock.systemUTC(), stateValidator);

			StepVerifier.create(mgr.authenticate(token))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.SESSION_EXPIRED);
					});
		}

		@Test
		@DisplayName("fails with TOKEN_BLACKLISTED when blacklist check fails")
		void failsWithTokenBlacklistedWhenBlacklistCheckFails() {
			Instant now = Instant.now();
			String jti = UUID.randomUUID().toString();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.id(jti)
					.claim("userId", "user-123")
					.claim("sessionId", "session-abc")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			JwtStateValidator stateValidator = Mockito.mock(JwtStateValidator.class);
			when(stateValidator.validate(any(), any(), any()))
					.thenReturn(Mono.error(new JwtAuthenticationException(
							GatewayAuthErrorCode.TOKEN_BLACKLISTED, "Token blacklisted")));

			JwtAuthenticationManager mgr = new JwtAuthenticationManager(
					properties, claimsMapper(), Clock.systemUTC(), stateValidator);

			StepVerifier.create(mgr.authenticate(token))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.TOKEN_BLACKLISTED);
					});
		}

		@Test
		@DisplayName("fails with USER_DISABLED when user enabled check fails")
		void failsWithUserDisabledWhenUserEnabledCheckFails() {
			Instant now = Instant.now();
			String jti = UUID.randomUUID().toString();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.id(jti)
					.claim("userId", "user-123")
					.claim("sessionId", "session-abc")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			JwtStateValidator stateValidator = Mockito.mock(JwtStateValidator.class);
			when(stateValidator.validate(any(), any(), any()))
					.thenReturn(Mono.error(new JwtAuthenticationException(
							GatewayAuthErrorCode.USER_DISABLED, "User disabled")));

			JwtAuthenticationManager mgr = new JwtAuthenticationManager(
					properties, claimsMapper(), Clock.systemUTC(), stateValidator);

			StepVerifier.create(mgr.authenticate(token))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.USER_DISABLED);
					});
		}

		@Test
		@DisplayName("passes jti from JWT claims to state validator")
		void passesJtiToValidator() {
			Instant now = Instant.now();
			String jti = UUID.randomUUID().toString();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.id(jti)
					.claim("userId", "user-123")
					.claim("sessionId", "session-abc")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			JwtStateValidator stateValidator = Mockito.mock(JwtStateValidator.class);
			when(stateValidator.validate("session-abc", jti, "user-123"))
					.thenReturn(Mono.empty());

			JwtAuthenticationManager mgr = new JwtAuthenticationManager(
					properties, claimsMapper(), Clock.systemUTC(), stateValidator);

			StepVerifier.create(mgr.authenticate(token))
					.assertNext(user -> assertThat(user.userId()).isEqualTo("user-123"))
					.verifyComplete();
		}
	}
}
