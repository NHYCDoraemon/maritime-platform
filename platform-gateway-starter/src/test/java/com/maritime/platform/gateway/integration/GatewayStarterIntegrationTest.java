package com.maritime.platform.gateway.integration;

import com.maritime.platform.gateway.autoconfigure.GatewayAutoConfiguration;
import com.maritime.platform.gateway.autoconfigure.GatewayRedisConfiguration;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.RouteSecurityPolicyResolver;
import com.maritime.platform.gateway.security.AuthMode;
import com.maritime.platform.gateway.security.RouteSecurityPolicy;
import com.maritime.platform.gateway.security.hmac.AppCredential;
import com.maritime.platform.gateway.security.hmac.AppCredentialResolver;
import com.maritime.platform.gateway.security.hmac.DefaultAppCredentialResolver;
import com.maritime.platform.gateway.security.hmac.HmacNonceValidator;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationManager;
import com.maritime.platform.gateway.security.jwt.JwtStateValidator;
import com.maritime.platform.gateway.security.nonce.JwtNonceValidator;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Gateway starter integration tests")
class GatewayStarterIntegrationTest {

    private static GenericContainer<?> redisContainer;

    @BeforeAll
    static void startRedis() {
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        redisContainer.start();
    }

    @AfterAll
    static void stopRedis() {
        if (redisContainer != null) {
            redisContainer.stop();
        }
    }

    private ApplicationContextRunner createRunner(String... propertyValues) {
        var beans = new ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        GatewayAutoConfiguration.class,
                        GatewayRedisConfiguration.class))
                .withUserConfiguration(TestRedisConfig.class)
                .withPropertyValues("spring.cloud.gateway.enabled=false",
                        "spring.main.web-application-type=none",
                        "maritime.gateway.security.default-auth-mode=none",
                        "maritime.gateway.security.jwt.enabled=true",
                        "maritime.gateway.security.jwt.secret=test-secret-with-minimum-length-256-bits!!",
                        "maritime.gateway.security.jwt.issuer=test-issuer",
                        "maritime.gateway.security.jwt.validation.require-session=true",
                        "maritime.gateway.security.jwt.validation.check-blacklist=true",
                        "maritime.gateway.security.jwt.validation.check-user-enabled=true",
                        "maritime.gateway.security.jwt.validation.user-enabled-disabled-value=DISABLED",
                        "maritime.gateway.security.jwt.nonce.enabled=true",
                        "maritime.gateway.security.hmac.enabled=true",
                        "maritime.gateway.security.hmac.credentials.apps[0].app-key=test-app",
                        "maritime.gateway.security.hmac.credentials.apps[0].app-secret=test-secret",
                        "maritime.gateway.security.hmac.credentials.apps[0].app-code=test-app-code")
                .withPropertyValues(propertyValues);
        return beans;
    }

    static class TestRedisConfig {
        @Bean
        ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
            return new LettuceConnectionFactory(
                    redisContainer.getHost(),
                    redisContainer.getMappedPort(6379));
        }

        @Bean
        AppCredentialResolver appCredentialResolver(GatewaySecurityProperties properties,
                @Autowired(required = false) ReactiveRedisTemplate<String, String> redisTemplate) {
            return new DefaultAppCredentialResolver(properties, redisTemplate);
        }
    }

    // ---------- startup verification ----------

    @Nested
    @DisplayName("Context startup")
    class ContextStartup {

        @Test
        @DisplayName("application context loads successfully with JWT and HMAC enabled")
        void contextLoads() {
            createRunner().run(ctx -> {
                assertThat(ctx).isNotNull();
                assertThat(ctx).hasNotFailed();
            });
        }

        @Test
        @DisplayName("ReactiveRedisTemplate bean is created")
        void redisTemplateIsAvailable() {
            createRunner().run(ctx -> {
                assertThat(ctx).hasSingleBean(ReactiveRedisTemplate.class);
            });
        }

        @Test
        @DisplayName("LettuceConnectionFactory connects to test Redis")
        void redisConnectionWorks() {
            createRunner().run(ctx -> {
                var factory = ctx.getBean(LettuceConnectionFactory.class);
                assertThat(factory).isNotNull();
            });
        }
    }

    // ---------- JWT nonce with real Redis ----------

    @Nested
    @DisplayName("JWT nonce validation")
    class JwtNonceValidation {

        @Test
        @DisplayName("first nonce use succeeds")
        void firstNonceSucceeds() {
            createRunner().run(ctx -> {
                var validator = ctx.getBean(JwtNonceValidator.class);
                StepVerifier.create(validator.validate("session-first", "nonce-alpha"))
                        .verifyComplete();
            });
        }

        @Test
        @DisplayName("replay of same nonce for same session fails with REPLAY_DETECTED")
        void replayDetected() {
            createRunner().run(ctx -> {
                var validator = ctx.getBean(JwtNonceValidator.class);
                String sessionId = "session-replay";
                String nonce = "nonce-replay";

                StepVerifier.create(validator.validate(sessionId, nonce))
                        .verifyComplete();

                StepVerifier.create(validator.validate(sessionId, nonce))
                        .verifyErrorSatisfies(e -> assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.REPLAY_DETECTED));
            });
        }

        @Test
        @DisplayName("same nonce with different session succeeds")
        void sameNonceDifferentSession() {
            createRunner().run(ctx -> {
                var validator = ctx.getBean(JwtNonceValidator.class);
                String nonce = "nonce-shared";

                StepVerifier.create(validator.validate("session-a", nonce))
                        .verifyComplete();
                StepVerifier.create(validator.validate("session-b", nonce))
                        .verifyComplete();
            });
        }

        @Test
        @DisplayName("different nonces for same session all succeed")
        void differentNoncesSucceed() {
            createRunner().run(ctx -> {
                var validator = ctx.getBean(JwtNonceValidator.class);

                StepVerifier.create(validator.validate("session-multi", "nonce-001"))
                        .verifyComplete();
                StepVerifier.create(validator.validate("session-multi", "nonce-002"))
                        .verifyComplete();
                StepVerifier.create(validator.validate("session-multi", "nonce-003"))
                        .verifyComplete();
            });
        }
    }

    // ---------- HMAC nonce with real Redis ----------

    @Nested
    @DisplayName("HMAC nonce validation")
    class HmacNonceValidation {

        @Test
        @DisplayName("first nonce use succeeds")
        void firstNonceSucceeds() {
            createRunner().run(ctx -> {
                var validator = ctx.getBean(HmacNonceValidator.class);
                StepVerifier.create(validator.validate("app-key-1", "hmac-nonce-alpha"))
                        .verifyComplete();
            });
        }

        @Test
        @DisplayName("replay of same nonce for same app fails with REPLAY_DETECTED")
        void replayDetected() {
            createRunner().run(ctx -> {
                var validator = ctx.getBean(HmacNonceValidator.class);
                String appKey = "app-key-2";
                String nonce = "hmac-nonce-replay";

                StepVerifier.create(validator.validate(appKey, nonce))
                        .verifyComplete();

                StepVerifier.create(validator.validate(appKey, nonce))
                        .verifyErrorSatisfies(e -> assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.REPLAY_DETECTED));
            });
        }

        @Test
        @DisplayName("same nonce with different app key succeeds")
        void sameNonceDifferentApp() {
            createRunner().run(ctx -> {
                var validator = ctx.getBean(HmacNonceValidator.class);
                String nonce = "hmac-nonce-cross-app";

                StepVerifier.create(validator.validate("app-a", nonce))
                        .verifyComplete();
                StepVerifier.create(validator.validate("app-b", nonce))
                        .verifyComplete();
            });
        }
    }

    // ---------- JWT state validation with real Redis ----------

    @Nested
    @DisplayName("JWT state validation")
    class JwtStateValidation {

        @Test
        @DisplayName("valid session passes validation")
        void validSessionPasses() {
            createRunner().run(ctx -> {
                var validator = ctx.getBean(JwtStateValidator.class);
                var props = ctx.getBean(GatewaySecurityProperties.class);
                var redisOps = ctx.getBean(ReactiveRedisOperations.class);

                String sessionId = "valid-session";
                String sessionKey = props.getJwt().getRedisKeys().getSessionPrefix() + sessionId;
                ((ReactiveRedisOperations<String, String>) redisOps).opsForValue().set(sessionKey, "1").block();

                StepVerifier.create(validator.validate(sessionId, null, "user-ok"))
                        .verifyComplete();

                ((ReactiveRedisOperations<String, String>) redisOps).delete(sessionKey).block();
            });
        }

        @Test
        @DisplayName("missing session fails with SESSION_EXPIRED")
        void missingSessionFails() {
            createRunner().run(ctx -> {
                var validator = ctx.getBean(JwtStateValidator.class);

                StepVerifier.create(validator.validate("nonexistent-session", null, "user-ok"))
                        .verifyErrorSatisfies(e -> assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.SESSION_EXPIRED));
            });
        }

        @Test
        @DisplayName("blacklisted token fails with TOKEN_BLACKLISTED")
        void blacklistedTokenFails() {
            createRunner().run(ctx -> {
                var validator = ctx.getBean(JwtStateValidator.class);
                var props = ctx.getBean(GatewaySecurityProperties.class);
                var redisOps = (ReactiveRedisOperations<String, String>) ctx.getBean(ReactiveRedisOperations.class);

                String jti = "blacklisted-jti";
                String sessionId = "session-bl";
                String blacklistKey = props.getJwt().getRedisKeys().getBlacklistPrefix() + jti;
                String sessionKey = props.getJwt().getRedisKeys().getSessionPrefix() + sessionId;

                redisOps.opsForValue().set(sessionKey, "1").block();
                redisOps.opsForValue().set(blacklistKey, "1").block();

                StepVerifier.create(validator.validate(sessionId, jti, "user-ok"))
                        .verifyErrorSatisfies(e -> assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.TOKEN_BLACKLISTED));

                redisOps.delete(sessionKey).block();
                redisOps.delete(blacklistKey).block();
            });
        }

        @Test
        @DisplayName("disabled user fails with USER_DISABLED")
        void disabledUserFails() {
            createRunner().run(ctx -> {
                var validator = ctx.getBean(JwtStateValidator.class);
                var props = ctx.getBean(GatewaySecurityProperties.class);
                var redisOps = (ReactiveRedisOperations<String, String>) ctx.getBean(ReactiveRedisOperations.class);

                String sessionId = "session-du";
                String sessionKey = props.getJwt().getRedisKeys().getSessionPrefix() + sessionId;
                String userKey = props.getJwt().getRedisKeys().getUserEnabledPrefix() + "disabled-user";

                redisOps.opsForValue().set(sessionKey, "1").block();
                redisOps.opsForValue().set(userKey, "DISABLED").block();

                StepVerifier.create(validator.validate(sessionId, null, "disabled-user"))
                        .verifyErrorSatisfies(e -> assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.USER_DISABLED));

                redisOps.delete(sessionKey).block();
                redisOps.delete(userKey).block();
            });
        }
    }

    // ---------- app credential resolution ----------

    @Nested
    @DisplayName("App credential resolution")
    class AppCredentialResolution {

        @Test
        @DisplayName("resolves app from Redis hash")
        void resolvesFromRedis() {
            createRunner().run(ctx -> {
                var resolver = ctx.getBean(DefaultAppCredentialResolver.class);
                var props = ctx.getBean(GatewaySecurityProperties.class);
                var redisOps = (ReactiveRedisOperations<String, String>) ctx.getBean(ReactiveRedisOperations.class);

                String appKey = "redis-app-001";
                String redisKey = props.getHmac().getCredentials().getRedisKeyPrefix() + appKey;

                redisOps.opsForHash().putAll(redisKey, Map.of(
                        "appSecret", "redis-app-secret",
                        "appCode", "redis-app-code",
                        "appId", "redis-app-id",
                        "tenantId", "tenant-redis",
                        "tenantCode", "TR001",
                        "permissions", "read,write",
                        "isEnabled", "true"
                )).block();

                StepVerifier.create(resolver.resolve(appKey))
                        .assertNext(credential -> {
                            assertThat(credential.getAppKey()).isEqualTo(appKey);
                            assertThat(credential.getAppSecret()).isEqualTo("redis-app-secret");
                            assertThat(credential.getAppCode()).isEqualTo("redis-app-code");
                            assertThat(credential.isEnabled()).isTrue();
                        })
                        .verifyComplete();

                redisOps.delete(redisKey).block();
            });
        }

        @Test
        @DisplayName("disabled app in Redis returns APP_DISABLED")
        void disabledAppInRedis() {
            createRunner().run(ctx -> {
                var resolver = ctx.getBean(DefaultAppCredentialResolver.class);
                var props = ctx.getBean(GatewaySecurityProperties.class);
                var redisOps = (ReactiveRedisOperations<String, String>) ctx.getBean(ReactiveRedisOperations.class);

                String appKey = "redis-disabled-app";
                String redisKey = props.getHmac().getCredentials().getRedisKeyPrefix() + appKey;

                redisOps.opsForHash().putAll(redisKey, Map.of(
                        "appSecret", "secret",
                        "appCode", "code",
                        "isEnabled", "false"
                )).block();

                StepVerifier.create(resolver.resolve(appKey))
                        .verifyErrorSatisfies(e -> assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.APP_DISABLED));

                redisOps.delete(redisKey).block();
            });
        }

        @Test
        @DisplayName("falls back to config when Redis has no entry")
        void fallsBackToConfig() {
            createRunner().run(ctx -> {
                var resolver = ctx.getBean(DefaultAppCredentialResolver.class);

                StepVerifier.create(resolver.resolve("test-app"))
                        .assertNext(credential -> {
                            assertThat(credential.getAppKey()).isEqualTo("test-app");
                            assertThat(credential.getAppSecret()).isEqualTo("test-secret");
                            assertThat(credential.getAppCode()).isEqualTo("test-app-code");
                        })
                        .verifyComplete();
            });
        }

        @Test
        @DisplayName("unknown app returns UNKNOWN_APP")
        void unknownApp() {
            createRunner().run(ctx -> {
                var resolver = ctx.getBean(DefaultAppCredentialResolver.class);

                StepVerifier.create(resolver.resolve("nonexistent-app"))
                        .verifyErrorSatisfies(e -> assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.UNKNOWN_APP));
            });
        }
    }

    // ---------- Route policy resolver: all four modes ----------

    @Nested
    @DisplayName("Route policy resolution (4 auth modes)")
    class RoutePolicyModes {

        @Test
        @DisplayName("default auth mode is NONE")
        void defaultModeIsNone() {
            createRunner().run(ctx -> {
                var resolver = ctx.getBean(RouteSecurityPolicyResolver.class);
                assertThat(resolver.getDefaultAuthMode()).isEqualTo(AuthMode.NONE);
            });
        }

        @Test
        @DisplayName("JWT auth mode via programmatic route policy")
        void jwtMode() {
            GatewaySecurityProperties props = new GatewaySecurityProperties();
            props.setDefaultAuthMode(AuthMode.NONE);
            props.getPublicPaths().add("/public/**");
            props.getJwt().setEnabled(true);
            props.getJwt().setSecret("test-secret");
            props.getJwt().setIssuer("test-issuer");

            RouteSecurityPolicyResolver r = new RouteSecurityPolicyResolver(props);
            r.addRoutePolicy("jwt-route", java.util.List.of("/api/**"), java.util.List.of(), AuthMode.JWT);
            r.afterPropertiesSet();

            RouteSecurityPolicy policy = r.resolve("/api/data", "POST");
            assertThat(policy.getAuthMode()).isEqualTo(AuthMode.JWT);
            assertThat(policy.getMatchedRuleId()).isEqualTo("jwt-route");
        }

        @Test
        @DisplayName("HMAC auth mode via programmatic route policy")
        void hmacMode() {
            GatewaySecurityProperties props = new GatewaySecurityProperties();
            props.setDefaultAuthMode(AuthMode.NONE);
            props.getHmac().setEnabled(true);

            RouteSecurityPolicyResolver r = new RouteSecurityPolicyResolver(props);
            r.addRoutePolicy("hmac-route", java.util.List.of("/openapi/**"), java.util.List.of(), AuthMode.HMAC);
            r.afterPropertiesSet();

            RouteSecurityPolicy policy = r.resolve("/openapi/data", "GET");
            assertThat(policy.getAuthMode()).isEqualTo(AuthMode.HMAC);
        }

        @Test
        @DisplayName("JWT_OR_HMAC auth mode via programmatic route policy")
        void jwtOrHmacMode() {
            GatewaySecurityProperties props = new GatewaySecurityProperties();
            props.setDefaultAuthMode(AuthMode.NONE);
            props.getJwt().setEnabled(true);
            props.getJwt().setSecret("test-secret");
            props.getJwt().setIssuer("test-issuer");
            props.getHmac().setEnabled(true);

            RouteSecurityPolicyResolver r = new RouteSecurityPolicyResolver(props);
            r.addRoutePolicy("dual-route", java.util.List.of("/dual/**"), java.util.List.of(), AuthMode.JWT_OR_HMAC);
            r.afterPropertiesSet();

            RouteSecurityPolicy policy = r.resolve("/dual/resource", "POST");
            assertThat(policy.getAuthMode()).isEqualTo(AuthMode.JWT_OR_HMAC);
        }

        @Test
        @DisplayName("NONE auth mode on public path")
        void noneMode() {
            GatewaySecurityProperties props = new GatewaySecurityProperties();
            props.setDefaultAuthMode(AuthMode.JWT);
            props.getPublicPaths().add("/public/**");

            RouteSecurityPolicyResolver r = new RouteSecurityPolicyResolver(props);
            r.afterPropertiesSet();

            RouteSecurityPolicy policy = r.resolve("/public/health", "GET");
            assertThat(policy.getAuthMode()).isEqualTo(AuthMode.NONE);
        }
    }

    // ---------- default config startup ----------

    @Nested
    @DisplayName("Default configuration")
    class DefaultConfiguration {

        @Test
        @DisplayName("context starts with only default properties (no JWT or HMAC enabled)")
        void defaultConfigStarts() {
            new ApplicationContextRunner()
                    .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                            GatewayAutoConfiguration.class,
                            GatewayRedisConfiguration.class))
                    .withUserConfiguration(TestRedisConfig.class)
                    .withPropertyValues("spring.profiles.active=integration-test",
                            "spring.cloud.gateway.enabled=false",
                            "spring.main.web-application-type=none",
                            "maritime.gateway.security.default-auth-mode=none")
                    .run(ctx -> {
                        assertThat(ctx).hasNotFailed();
                        assertThat(ctx).hasSingleBean(GatewaySecurityProperties.class);
                    });
        }
    }

    // ---------- JWT auth manager state validation through auto-wired bean ----------

    @Nested
    @DisplayName("JWT authentication manager state validation (auto-wired)")
    class JwtAuthManagerStateValidation {

        private static final String SECRET = "test-secret-with-minimum-length-256-bits!!";
        private static final String ISSUER = "test-issuer";

        private SecretKey signingKey() {
            return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        }

        private String createToken(String userId, String sessionId, String jti) {
            Instant now = Instant.now();
            return Jwts.builder()
                    .issuer(ISSUER)
                    .id(jti)
                    .claim("userId", userId)
                    .claim("sessionId", sessionId)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(3600)))
                    .signWith(signingKey())
                    .compact();
        }

        @Test
        @DisplayName("valid token with Redis session succeeds")
        void validTokenWithSessionSucceeds() {
            createRunner().run(ctx -> {
                var manager = ctx.getBean(JwtAuthenticationManager.class);
                var props = ctx.getBean(GatewaySecurityProperties.class);
                var redisOps = (ReactiveRedisOperations<String, String>) ctx.getBean(ReactiveRedisOperations.class);

                String sessionId = "auto-session-ok";
                String sessionKey = props.getJwt().getRedisKeys().getSessionPrefix() + sessionId;
                redisOps.opsForValue().set(sessionKey, "1").block();

                String token = createToken("user-ok", sessionId, UUID.randomUUID().toString());

                StepVerifier.create(manager.authenticate(token))
                        .assertNext(user -> {
                            assertThat(user.userId()).isEqualTo("user-ok");
                            assertThat(user.sessionId()).isEqualTo(sessionId);
                        })
                        .verifyComplete();

                redisOps.delete(sessionKey).block();
            });
        }

        @Test
        @DisplayName("valid token but Redis has no session returns SESSION_EXPIRED")
        void missingSessionReturnsSessionExpired() {
            createRunner().run(ctx -> {
                var manager = ctx.getBean(JwtAuthenticationManager.class);

                String token = createToken("user-nosession", "nonexistent-session-xyz",
                        UUID.randomUUID().toString());

                StepVerifier.create(manager.authenticate(token))
                        .verifyErrorSatisfies(e -> assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.SESSION_EXPIRED));
            });
        }

        @Test
        @DisplayName("blacklisted token returns TOKEN_BLACKLISTED")
        void blacklistedTokenReturnsTokenBlacklisted() {
            createRunner().run(ctx -> {
                var manager = ctx.getBean(JwtAuthenticationManager.class);
                var props = ctx.getBean(GatewaySecurityProperties.class);
                var redisOps = (ReactiveRedisOperations<String, String>) ctx.getBean(ReactiveRedisOperations.class);

                String sessionId = "auto-session-bl";
                String jti = UUID.randomUUID().toString();
                String sessionKey = props.getJwt().getRedisKeys().getSessionPrefix() + sessionId;
                String blacklistKey = props.getJwt().getRedisKeys().getBlacklistPrefix() + jti;

                redisOps.opsForValue().set(sessionKey, "1").block();
                redisOps.opsForValue().set(blacklistKey, "1").block();

                String token = createToken("user-bl", sessionId, jti);

                StepVerifier.create(manager.authenticate(token))
                        .verifyErrorSatisfies(e -> assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.TOKEN_BLACKLISTED));

                redisOps.delete(sessionKey, blacklistKey).block();
            });
        }

        @Test
        @DisplayName("disabled user returns USER_DISABLED")
        void disabledUserReturnsUserDisabled() {
            createRunner().run(ctx -> {
                var manager = ctx.getBean(JwtAuthenticationManager.class);
                var props = ctx.getBean(GatewaySecurityProperties.class);
                var redisOps = (ReactiveRedisOperations<String, String>) ctx.getBean(ReactiveRedisOperations.class);

                String userId = "user-disabled-001";
                String sessionId = "auto-session-du";
                String sessionKey = props.getJwt().getRedisKeys().getSessionPrefix() + sessionId;
                String userKey = props.getJwt().getRedisKeys().getUserEnabledPrefix() + userId;

                redisOps.opsForValue().set(sessionKey, "1").block();
                redisOps.opsForValue().set(userKey, "DISABLED").block();

                String token = createToken(userId, sessionId, UUID.randomUUID().toString());

                StepVerifier.create(manager.authenticate(token))
                        .verifyErrorSatisfies(e -> assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.USER_DISABLED));

                redisOps.delete(sessionKey, userKey).block();
            });
        }
    }
}
