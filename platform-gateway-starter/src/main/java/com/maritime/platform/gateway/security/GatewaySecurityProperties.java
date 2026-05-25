package com.maritime.platform.gateway.security;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "maritime.gateway.security")
@Validated
public class GatewaySecurityProperties implements InitializingBean {

    private AuthMode defaultAuthMode = AuthMode.JWT;

    private List<String> publicPaths = new ArrayList<>();

    @Valid
    private List<RoutePolicy> routes = new ArrayList<>();

    @Valid
    private Jwt jwt = new Jwt();

    @Valid
    private Hmac hmac = new Hmac();

    public AuthMode getDefaultAuthMode() { return defaultAuthMode; }
    public void setDefaultAuthMode(AuthMode defaultAuthMode) { this.defaultAuthMode = defaultAuthMode; }

    public List<String> getPublicPaths() { return publicPaths; }
    public void setPublicPaths(List<String> publicPaths) { this.publicPaths = publicPaths; }

    public List<RoutePolicy> getRoutes() { return routes; }
    public void setRoutes(List<RoutePolicy> routes) { this.routes = routes; }

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }

    public Hmac getHmac() { return hmac; }
    public void setHmac(Hmac hmac) { this.hmac = hmac; }

    @Override
    public void afterPropertiesSet() {
        if (jwt.enabled) {
            if (jwt.secret == null || jwt.secret.isBlank()) {
                throw new IllegalStateException(
                        "maritime.gateway.security.jwt.secret must not be blank when jwt.enabled=true");
            }
            if (jwt.issuer == null || jwt.issuer.isBlank()) {
                throw new IllegalStateException(
                        "maritime.gateway.security.jwt.issuer must not be blank when jwt.enabled=true");
            }
        }
        if (hmac.enabled) {
            if (hmac.timestampTolerance == null || hmac.timestampTolerance.isNegative()) {
                throw new IllegalStateException(
                        "maritime.gateway.security.hmac.timestamp-tolerance must be a positive duration when hmac.enabled=true");
            }
            if (hmac.nonceTtl == null || hmac.nonceTtl.isNegative()) {
                throw new IllegalStateException(
                        "maritime.gateway.security.hmac.nonce-ttl must be a positive duration when hmac.enabled=true");
            }
        }
    }

    // ---------- nested types ----------

    @Validated
    public static class RoutePolicy {

        @NotBlank
        private String id;

        @NotEmpty
        private List<String> paths = new ArrayList<>();

        private List<String> methods = new ArrayList<>();

        private AuthMode authMode;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public List<String> getPaths() { return paths; }
        public void setPaths(List<String> paths) { this.paths = paths; }
        public List<String> getMethods() { return methods; }
        public void setMethods(List<String> methods) { this.methods = methods; }
        public AuthMode getAuthMode() { return authMode; }
        public void setAuthMode(AuthMode authMode) { this.authMode = authMode; }
    }

    @Validated
    public static class Jwt {

        private boolean enabled;

        private String secret;

        private boolean encrypted;

        private String issuer = "";

        @Min(0)
        private int clockSkewSeconds = 30;

        @Valid
        private Claims claims = new Claims();

        @Valid
        private RedisKeys redisKeys = new RedisKeys();

        @Valid
        private Validation validation = new Validation();

        @Valid
        private JwtNonce nonce = new JwtNonce();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public boolean isEncrypted() { return encrypted; }
        public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public int getClockSkewSeconds() { return clockSkewSeconds; }
        public void setClockSkewSeconds(int clockSkewSeconds) { this.clockSkewSeconds = clockSkewSeconds; }
        public Claims getClaims() { return claims; }
        public void setClaims(Claims claims) { this.claims = claims; }
        public RedisKeys getRedisKeys() { return redisKeys; }
        public void setRedisKeys(RedisKeys redisKeys) { this.redisKeys = redisKeys; }
        public Validation getValidation() { return validation; }
        public void setValidation(Validation validation) { this.validation = validation; }
        public JwtNonce getNonce() { return nonce; }
        public void setNonce(JwtNonce nonce) { this.nonce = nonce; }
    }

    public static class Claims {

        private String userId = "userId";
        private String userName = "userName";
        private String sessionId = "sessionId";
        private String activeOrgCode = "activeOrgCode";
        private String activeOrgName = "activeOrgName";
        private String systemScope = "systemScope";
        private String userSource = "userSource";
        private String tenantId = "tenantId";

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getActiveOrgCode() { return activeOrgCode; }
        public void setActiveOrgCode(String activeOrgCode) { this.activeOrgCode = activeOrgCode; }
        public String getActiveOrgName() { return activeOrgName; }
        public void setActiveOrgName(String activeOrgName) { this.activeOrgName = activeOrgName; }
        public String getSystemScope() { return systemScope; }
        public void setSystemScope(String systemScope) { this.systemScope = systemScope; }
        public String getUserSource() { return userSource; }
        public void setUserSource(String userSource) { this.userSource = userSource; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }

    public static class RedisKeys {

        private String sessionPrefix = "iam:session:";
        private String blacklistPrefix = "iam:token:blacklist:";
        private String userEnabledPrefix = "iam:user:enabled:";

        public String getSessionPrefix() { return sessionPrefix; }
        public void setSessionPrefix(String sessionPrefix) { this.sessionPrefix = sessionPrefix; }
        public String getBlacklistPrefix() { return blacklistPrefix; }
        public void setBlacklistPrefix(String blacklistPrefix) { this.blacklistPrefix = blacklistPrefix; }
        public String getUserEnabledPrefix() { return userEnabledPrefix; }
        public void setUserEnabledPrefix(String userEnabledPrefix) { this.userEnabledPrefix = userEnabledPrefix; }
    }

    public static class Validation {

        private boolean requireSession = true;
        private boolean checkBlacklist = true;
        private boolean checkUserEnabled = true;

        @NotNull
        private String userEnabledDisabledValue = "0";

        public boolean isRequireSession() { return requireSession; }
        public void setRequireSession(boolean requireSession) { this.requireSession = requireSession; }
        public boolean isCheckBlacklist() { return checkBlacklist; }
        public void setCheckBlacklist(boolean checkBlacklist) { this.checkBlacklist = checkBlacklist; }
        public boolean isCheckUserEnabled() { return checkUserEnabled; }
        public void setCheckUserEnabled(boolean checkUserEnabled) { this.checkUserEnabled = checkUserEnabled; }
        public String getUserEnabledDisabledValue() { return userEnabledDisabledValue; }
        public void setUserEnabledDisabledValue(String userEnabledDisabledValue) { this.userEnabledDisabledValue = userEnabledDisabledValue; }
    }

    @Validated
    public static class JwtNonce {

        private boolean enabled = true;

        @NotNull
        private String mode = "simple-setnx";

        private List<String> requiredMethods = List.of("POST", "PUT", "PATCH", "DELETE");

        @NotNull
        private Duration ttl = Duration.of(5, ChronoUnit.MINUTES);

        private String simpleKeyPrefix = "platform:gateway:jwt:nonce:";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public List<String> getRequiredMethods() { return requiredMethods; }
        public void setRequiredMethods(List<String> requiredMethods) { this.requiredMethods = requiredMethods; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
        public String getSimpleKeyPrefix() { return simpleKeyPrefix; }
        public void setSimpleKeyPrefix(String simpleKeyPrefix) { this.simpleKeyPrefix = simpleKeyPrefix; }
    }

    @Validated
    public static class Hmac {

        private boolean enabled;

        @NotNull
        private Duration timestampTolerance = Duration.of(5, ChronoUnit.MINUTES);

        @Min(1)
        private int minNonceLength = 16;

        private String nonceKeyPrefix = "platform:gateway:hmac:nonce:";

        @NotNull
        private Duration nonceTtl = Duration.of(5, ChronoUnit.MINUTES);

        @Valid
        private Headers headers = new Headers();

        @Valid
        private Credentials credentials = new Credentials();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Duration getTimestampTolerance() { return timestampTolerance; }
        public void setTimestampTolerance(Duration timestampTolerance) { this.timestampTolerance = timestampTolerance; }
        public int getMinNonceLength() { return minNonceLength; }
        public void setMinNonceLength(int minNonceLength) { this.minNonceLength = minNonceLength; }
        public String getNonceKeyPrefix() { return nonceKeyPrefix; }
        public void setNonceKeyPrefix(String nonceKeyPrefix) { this.nonceKeyPrefix = nonceKeyPrefix; }
        public Duration getNonceTtl() { return nonceTtl; }
        public void setNonceTtl(Duration nonceTtl) { this.nonceTtl = nonceTtl; }
        public Headers getHeaders() { return headers; }
        public void setHeaders(Headers headers) { this.headers = headers; }
        public Credentials getCredentials() { return credentials; }
        public void setCredentials(Credentials credentials) { this.credentials = credentials; }
    }

    public static class Headers {

        private String appKey = "X-App-Key";
        private String timestamp = "X-Timestamp";
        private String nonce = "X-Nonce";
        private String bodyDigest = "X-Body-Digest";
        private String signature = "X-Signature";

        public String getAppKey() { return appKey; }
        public void setAppKey(String appKey) { this.appKey = appKey; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public String getNonce() { return nonce; }
        public void setNonce(String nonce) { this.nonce = nonce; }
        public String getBodyDigest() { return bodyDigest; }
        public void setBodyDigest(String bodyDigest) { this.bodyDigest = bodyDigest; }
        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
    }

    @Validated
    public static class Credentials {

        private String source = "redis-with-config-fallback";

        private String redisKeyPrefix = "iam:app:auth:";

        @Valid
        private CredentialFields fields = new CredentialFields();

        @Valid
        private List<ConfigApp> apps = new ArrayList<>();

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getRedisKeyPrefix() { return redisKeyPrefix; }
        public void setRedisKeyPrefix(String redisKeyPrefix) { this.redisKeyPrefix = redisKeyPrefix; }
        public CredentialFields getFields() { return fields; }
        public void setFields(CredentialFields fields) { this.fields = fields; }
        public List<ConfigApp> getApps() { return apps; }
        public void setApps(List<ConfigApp> apps) { this.apps = apps; }
    }

    public static class CredentialFields {

        private String appSecret = "appSecret";
        private String appCode = "appCode";
        private String appId = "appId";
        private String tenantId = "tenantId";
        private String tenantCode = "tenantCode";
        private String permissions = "permissions";
        private String enabled = "isEnabled";

        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
        public String getAppCode() { return appCode; }
        public void setAppCode(String appCode) { this.appCode = appCode; }
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getTenantCode() { return tenantCode; }
        public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
        public String getPermissions() { return permissions; }
        public void setPermissions(String permissions) { this.permissions = permissions; }
        public String getEnabled() { return enabled; }
        public void setEnabled(String enabled) { this.enabled = enabled; }
    }

    @Validated
    public static class ConfigApp {

        @NotBlank
        private String appKey;

        @NotBlank
        private String appSecret;

        @NotBlank
        private String appCode;

        private boolean enabled = true;

        public String getAppKey() { return appKey; }
        public void setAppKey(String appKey) { this.appKey = appKey; }
        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
        public String getAppCode() { return appCode; }
        public void setAppCode(String appCode) { this.appCode = appCode; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
