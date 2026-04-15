package com.maritime.platform.common.security.jwt;

import com.maritime.platform.common.core.exception.BusinessException;
import com.maritime.platform.common.core.result.ResultCode;
import com.maritime.platform.common.security.context.SecurityUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Creates and validates JWT tokens using JJWT (HS256).
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private SecretKey signingKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Creates an access token carrying the full user context.
     */
    public String createAccessToken(SecurityUser user) {
        Date now = new Date();
        long expirationMs = properties.getAccessTokenExpirationMinutes() * 60 * 1000;
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.userId())
                .claim("userId", user.userId())
                .claim("userName", user.userName())
                .claim("activeOrgCode", user.activeOrgCode())
                .claim("systemScope", user.systemScope())
                .claim("sessionId", user.sessionId())
                .issuer(properties.getIssuer())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Creates a refresh token with only the session identifier.
     */
    public String createRefreshToken(String sessionId) {
        Date now = new Date();
        long expirationMs = properties.getRefreshTokenExpirationHours() * 60 * 60 * 1000;
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claim("sessionId", sessionId)
                .issuer(properties.getIssuer())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and validates a JWT token, returning its claims.
     *
     * @throws BusinessException with {@code AUTH_TOKEN_EXPIRED} or {@code AUTH_TOKEN_INVALID}
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ResultCode.AUTH_TOKEN_EXPIRED, e);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.AUTH_TOKEN_INVALID, e);
        }
    }
}
