package com.maritime.platform.common.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized JWT configuration bound to {@code iam.security.jwt.*}.
 */
@ConfigurationProperties(prefix = "iam.security.jwt")
public class JwtProperties {

    private String secret = "iam-center-default-jwt-secret-key-change-in-production!";

    private long accessTokenExpirationMinutes = 30;

    private long refreshTokenExpirationHours = 24;

    private String issuer = "iam-center";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpirationMinutes() {
        return accessTokenExpirationMinutes;
    }

    public void setAccessTokenExpirationMinutes(long accessTokenExpirationMinutes) {
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
    }

    public long getRefreshTokenExpirationHours() {
        return refreshTokenExpirationHours;
    }

    public void setRefreshTokenExpirationHours(long refreshTokenExpirationHours) {
        this.refreshTokenExpirationHours = refreshTokenExpirationHours;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
