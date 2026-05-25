package com.maritime.platform.gateway.security.jwt;

import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/**
 * Extracts, decrypts, verifies and maps JWT tokens to
 * {@link GatewayPrincipal.User} instances.
 *
 * <p>All CPU-bound operations run via {@link Mono#fromCallable},
 * keeping the non-blocking gateway event loop free. State validation
 * (session, blacklist, user-enabled) is delegated to
 * {@link JwtStateValidator} and runs reactively.
 */
@Component
@ConditionalOnProperty("maritime.gateway.security.jwt.enabled")
public class JwtAuthenticationManager {

	private final GatewaySecurityProperties.Jwt jwtConfig;
	private final SecretKey signingKey;
	private final JwtEncryptor encryptor;
	private final JwtClaimsMapper claimsMapper;
	private final Clock clock;
	private final JwtStateValidator stateValidator;

	@Autowired
	public JwtAuthenticationManager(GatewaySecurityProperties properties, JwtClaimsMapper claimsMapper) {
		this(properties, claimsMapper, Clock.systemUTC(), null);
	}

	public JwtAuthenticationManager(GatewaySecurityProperties properties, JwtClaimsMapper claimsMapper,
			JwtStateValidator stateValidator) {
		this(properties, claimsMapper, Clock.systemUTC(), stateValidator);
	}

	JwtAuthenticationManager(GatewaySecurityProperties properties, JwtClaimsMapper claimsMapper, Clock clock) {
		this(properties, claimsMapper, clock, null);
	}

	JwtAuthenticationManager(GatewaySecurityProperties properties, JwtClaimsMapper claimsMapper, Clock clock,
			JwtStateValidator stateValidator) {
		this.jwtConfig = properties.getJwt();
		this.signingKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
		this.encryptor = jwtConfig.isEncrypted() ? new JwtEncryptor(jwtConfig.getSecret()) : null;
		this.claimsMapper = claimsMapper;
		this.clock = clock;
		this.stateValidator = stateValidator;
	}

	/**
	 * Authenticate the given token string, returning a mapped user principal
	 * or an error signal carrying a {@link JwtAuthenticationException}.
	 */
	public Mono<GatewayPrincipal.User> authenticate(String token) {
		return Mono.fromCallable(() -> doParseAndMap(token))
				.flatMap(result -> {
					if (stateValidator == null) {
						return Mono.just(result.user);
					}
					return stateValidator.validate(result.user.sessionId(), result.jti, result.user.userId())
							.thenReturn(result.user);
				});
	}

	private AuthResult doParseAndMap(String token) {
		String rawToken = decryptIfNeeded(token);
		Claims claims = parseAndValidate(rawToken);
		GatewayPrincipal.User user = claimsMapper.map(claims);
		validateRequiredClaims(user);
		String jti = claims.getId();
		return new AuthResult(user, jti);
	}

	private String decryptIfNeeded(String token) {
		if (encryptor == null) {
			return token;
		}
		String decrypted = encryptor.decrypt(token);
		if (decrypted == null) {
			throw new JwtAuthenticationException(GatewayAuthErrorCode.INVALID_TOKEN,
					"Token decryption failed");
		}
		return decrypted;
	}

	private Claims parseAndValidate(String token) {
		try {
			Jws<Claims> jws = Jwts.parser()
					.verifyWith(signingKey)
					.clockSkewSeconds(jwtConfig.getClockSkewSeconds())
					.clock(() -> Date.from(clock.instant()))
					.build()
					.parseSignedClaims(token);

			Claims claims = jws.getPayload();

			String issuer = claims.getIssuer();
			if (!jwtConfig.getIssuer().equals(issuer)) {
				throw new JwtAuthenticationException(GatewayAuthErrorCode.INVALID_TOKEN,
						"Invalid issuer: " + issuer);
			}

			Date expiration = claims.getExpiration();
			if (expiration != null) {
				Instant expiryInstant = expiration.toInstant();
				Instant threshold = clock.instant().minusSeconds(jwtConfig.getClockSkewSeconds());
				if (expiryInstant.isBefore(threshold)) {
					throw new JwtAuthenticationException(GatewayAuthErrorCode.TOKEN_EXPIRED,
							"Token expired at " + expiration);
				}
			}

			return claims;
		} catch (ExpiredJwtException e) {
			throw new JwtAuthenticationException(GatewayAuthErrorCode.TOKEN_EXPIRED, e.getMessage());
		} catch (JwtAuthenticationException e) {
			throw e;
		} catch (Exception e) {
			throw new JwtAuthenticationException(GatewayAuthErrorCode.INVALID_TOKEN, e.getMessage());
		}
	}

	private void validateRequiredClaims(GatewayPrincipal.User user) {
		if (!StringUtils.hasText(user.userId())) {
			throw new JwtAuthenticationException(GatewayAuthErrorCode.INVALID_TOKEN,
					"Required claim '" + jwtConfig.getClaims().getUserId() + "' is missing or empty");
		}
		if (!StringUtils.hasText(user.sessionId())) {
			throw new JwtAuthenticationException(GatewayAuthErrorCode.INVALID_TOKEN,
					"Required claim '" + jwtConfig.getClaims().getSessionId() + "' is missing or empty");
		}
	}

	/**
	 * Intermediate result holding the mapped user and the JWT ID for
	 * downstream state validation.
	 */
	private static class AuthResult {
		final GatewayPrincipal.User user;
		final String jti;

		AuthResult(GatewayPrincipal.User user, String jti) {
			this.user = user;
			this.jti = jti;
		}
	}
}
