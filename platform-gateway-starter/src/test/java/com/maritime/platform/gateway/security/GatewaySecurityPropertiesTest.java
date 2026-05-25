package com.maritime.platform.gateway.security;

import java.time.Duration;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GatewaySecurityProperties binding tests")
class GatewaySecurityPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GatewaySecurityProperties.class)
    static class TestConfiguration {
        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }

    @Nested
    @DisplayName("Default configuration")
    class Defaults {

        @Test
        @DisplayName("can start with no properties set")
        void startsWithDefaults() {
            runner.run(ctx -> {
                GatewaySecurityProperties props = ctx.getBean(GatewaySecurityProperties.class);

                assertThat(props.getDefaultAuthMode()).isEqualTo(AuthMode.JWT);
                assertThat(props.getPublicPaths()).isEmpty();
                assertThat(props.getRoutes()).isEmpty();

                assertThat(props.getJwt().isEnabled()).isFalse();
                assertThat(props.getJwt().getSecret()).isNull();
                assertThat(props.getJwt().isEncrypted()).isFalse();
                assertThat(props.getJwt().getIssuer()).isEmpty();
                assertThat(props.getJwt().getClockSkewSeconds()).isEqualTo(30);

                assertThat(props.getHmac().isEnabled()).isFalse();
                assertThat(props.getHmac().getTimestampTolerance()).isEqualTo(Duration.ofMinutes(5));
                assertThat(props.getHmac().getMinNonceLength()).isEqualTo(16);
                assertThat(props.getHmac().getNonceTtl()).isEqualTo(Duration.ofMinutes(5));
            });
        }

        @Test
        @DisplayName("claims have expected default field mappings")
        void claimsDefaults() {
            runner.run(ctx -> {
                GatewaySecurityProperties.Claims claims = ctx.getBean(GatewaySecurityProperties.class)
                        .getJwt().getClaims();

                assertThat(claims.getUserId()).isEqualTo("userId");
                assertThat(claims.getUserName()).isEqualTo("userName");
                assertThat(claims.getSessionId()).isEqualTo("sessionId");
                assertThat(claims.getActiveOrgCode()).isEqualTo("activeOrgCode");
                assertThat(claims.getActiveOrgName()).isEqualTo("activeOrgName");
                assertThat(claims.getSystemScope()).isEqualTo("systemScope");
                assertThat(claims.getUserSource()).isEqualTo("userSource");
                assertThat(claims.getTenantId()).isEqualTo("tenantId");
            });
        }

        @Test
        @DisplayName("redis key prefixes have platform defaults")
        void redisKeyDefaults() {
            runner.run(ctx -> {
                GatewaySecurityProperties.RedisKeys keys = ctx.getBean(GatewaySecurityProperties.class)
                        .getJwt().getRedisKeys();

                assertThat(keys.getSessionPrefix()).isEqualTo("iam:session:");
                assertThat(keys.getBlacklistPrefix()).isEqualTo("iam:token:blacklist:");
                assertThat(keys.getUserEnabledPrefix()).isEqualTo("iam:user:enabled:");
            });
        }

        @Test
        @DisplayName("validation defaults are all enabled")
        void validationDefaults() {
            runner.run(ctx -> {
                GatewaySecurityProperties.Validation v = ctx.getBean(GatewaySecurityProperties.class)
                        .getJwt().getValidation();

                assertThat(v.isRequireSession()).isTrue();
                assertThat(v.isCheckBlacklist()).isTrue();
                assertThat(v.isCheckUserEnabled()).isTrue();
                assertThat(v.getUserEnabledDisabledValue()).isEqualTo("0");
            });
        }

        @Test
        @DisplayName("JWT nonce defaults")
        void jwtNonceDefaults() {
            runner.run(ctx -> {
                GatewaySecurityProperties.JwtNonce n = ctx.getBean(GatewaySecurityProperties.class)
                        .getJwt().getNonce();

                assertThat(n.isEnabled()).isTrue();
                assertThat(n.getMode()).isEqualTo("simple-setnx");
                assertThat(n.getRequiredMethods()).containsExactly("POST", "PUT", "PATCH", "DELETE");
                assertThat(n.getTtl()).isEqualTo(Duration.ofMinutes(5));
                assertThat(n.getSimpleKeyPrefix()).isEqualTo("platform:gateway:jwt:nonce:");
            });
        }

        @Test
        @DisplayName("HMAC headers have standard defaults")
        void hmacHeaderDefaults() {
            runner.run(ctx -> {
                GatewaySecurityProperties.Headers h = ctx.getBean(GatewaySecurityProperties.class)
                        .getHmac().getHeaders();

                assertThat(h.getAppKey()).isEqualTo("X-App-Key");
                assertThat(h.getTimestamp()).isEqualTo("X-Timestamp");
                assertThat(h.getNonce()).isEqualTo("X-Nonce");
                assertThat(h.getBodyDigest()).isEqualTo("X-Body-Digest");
                assertThat(h.getSignature()).isEqualTo("X-Signature");
            });
        }

        @Test
        @DisplayName("HMAC credential fields have standard defaults")
        void hmacCredentialFieldDefaults() {
            runner.run(ctx -> {
                GatewaySecurityProperties.CredentialFields f = ctx.getBean(GatewaySecurityProperties.class)
                        .getHmac().getCredentials().getFields();

                assertThat(f.getAppSecret()).isEqualTo("appSecret");
                assertThat(f.getAppCode()).isEqualTo("appCode");
                assertThat(f.getAppId()).isEqualTo("appId");
                assertThat(f.getTenantId()).isEqualTo("tenantId");
                assertThat(f.getTenantCode()).isEqualTo("tenantCode");
                assertThat(f.getPermissions()).isEqualTo("permissions");
                assertThat(f.getEnabled()).isEqualTo("isEnabled");
            });
        }
    }

    @Nested
    @DisplayName("Full valid configuration")
    class FullConfiguration {

        @Test
        @DisplayName("binds all JWT settings from kebab-case YAML")
        void bindsJwtConfiguration() {
            runner.withPropertyValues(
                    "maritime.gateway.security.default-auth-mode=jwt",
                    "maritime.gateway.security.public-paths[0]=/actuator/**",
                    "maritime.gateway.security.public-paths[1]=/v3/api-docs/**",
                    "maritime.gateway.security.public-paths[2]=/swagger-ui/**",
                    "maritime.gateway.security.public-paths[3]=/doc.html",
                    "maritime.gateway.security.public-paths[4]=/webjars/**",
                    // JWT config
                    "maritime.gateway.security.jwt.enabled=true",
                    "maritime.gateway.security.jwt.secret=test-jwt-secret",
                    "maritime.gateway.security.jwt.encrypted=true",
                    "maritime.gateway.security.jwt.issuer=maritime-platform",
                    "maritime.gateway.security.jwt.clock-skew-seconds=60",
                    // claims
                    "maritime.gateway.security.jwt.claims.user-id=uid",
                    "maritime.gateway.security.jwt.claims.user-name=uname",
                    "maritime.gateway.security.jwt.claims.session-id=sid",
                    "maritime.gateway.security.jwt.claims.active-org-code=orgCode",
                    "maritime.gateway.security.jwt.claims.active-org-name=orgName",
                    "maritime.gateway.security.jwt.claims.system-scope=scope",
                    "maritime.gateway.security.jwt.claims.user-source=source",
                    "maritime.gateway.security.jwt.claims.tenant-id=tid",
                    // redis keys
                    "maritime.gateway.security.jwt.redis-keys.session-prefix=custom:session:",
                    "maritime.gateway.security.jwt.redis-keys.blacklist-prefix=custom:blacklist:",
                    "maritime.gateway.security.jwt.redis-keys.user-enabled-prefix=custom:enabled:",
                    // validation
                    "maritime.gateway.security.jwt.validation.require-session=false",
                    "maritime.gateway.security.jwt.validation.check-blacklist=false",
                    "maritime.gateway.security.jwt.validation.check-user-enabled=false",
                    "maritime.gateway.security.jwt.validation.user-enabled-disabled-value=-1",
                    // nonce
                    "maritime.gateway.security.jwt.nonce.enabled=false",
                    "maritime.gateway.security.jwt.nonce.mode=custom-mode",
                    "maritime.gateway.security.jwt.nonce.required-methods=POST,PUT",
                    "maritime.gateway.security.jwt.nonce.ttl=10m",
                    "maritime.gateway.security.jwt.nonce.simple-key-prefix=custom:nonce:"
            ).run(ctx -> {
                GatewaySecurityProperties props = ctx.getBean(GatewaySecurityProperties.class);

                assertThat(props.getDefaultAuthMode()).isEqualTo(AuthMode.JWT);
                assertThat(props.getPublicPaths()).containsExactly(
                        "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/doc.html", "/webjars/**");

                GatewaySecurityProperties.Jwt jwt = props.getJwt();
                assertThat(jwt.isEnabled()).isTrue();
                assertThat(jwt.getSecret()).isEqualTo("test-jwt-secret");
                assertThat(jwt.isEncrypted()).isTrue();
                assertThat(jwt.getIssuer()).isEqualTo("maritime-platform");
                assertThat(jwt.getClockSkewSeconds()).isEqualTo(60);

                assertThat(jwt.getClaims().getUserId()).isEqualTo("uid");
                assertThat(jwt.getClaims().getUserName()).isEqualTo("uname");
                assertThat(jwt.getClaims().getSessionId()).isEqualTo("sid");
                assertThat(jwt.getClaims().getActiveOrgCode()).isEqualTo("orgCode");
                assertThat(jwt.getClaims().getActiveOrgName()).isEqualTo("orgName");
                assertThat(jwt.getClaims().getSystemScope()).isEqualTo("scope");
                assertThat(jwt.getClaims().getUserSource()).isEqualTo("source");
                assertThat(jwt.getClaims().getTenantId()).isEqualTo("tid");

                assertThat(jwt.getRedisKeys().getSessionPrefix()).isEqualTo("custom:session:");
                assertThat(jwt.getRedisKeys().getBlacklistPrefix()).isEqualTo("custom:blacklist:");
                assertThat(jwt.getRedisKeys().getUserEnabledPrefix()).isEqualTo("custom:enabled:");

                assertThat(jwt.getValidation().isRequireSession()).isFalse();
                assertThat(jwt.getValidation().isCheckBlacklist()).isFalse();
                assertThat(jwt.getValidation().isCheckUserEnabled()).isFalse();
                assertThat(jwt.getValidation().getUserEnabledDisabledValue()).isEqualTo("-1");

                assertThat(jwt.getNonce().isEnabled()).isFalse();
                assertThat(jwt.getNonce().getMode()).isEqualTo("custom-mode");
                assertThat(jwt.getNonce().getRequiredMethods()).containsExactly("POST", "PUT");
                assertThat(jwt.getNonce().getTtl()).isEqualTo(Duration.ofMinutes(10));
                assertThat(jwt.getNonce().getSimpleKeyPrefix()).isEqualTo("custom:nonce:");
            });
        }

        @Test
        @DisplayName("binds all HMAC settings from kebab-case YAML")
        void bindsHmacConfiguration() {
            runner.withPropertyValues(
                    "maritime.gateway.security.hmac.enabled=true",
                    "maritime.gateway.security.hmac.timestamp-tolerance=10m",
                    "maritime.gateway.security.hmac.min-nonce-length=32",
                    "maritime.gateway.security.hmac.nonce-key-prefix=custom:hmac:nonce:",
                    "maritime.gateway.security.hmac.nonce-ttl=10m",
                    // headers
                    "maritime.gateway.security.hmac.headers.app-key=My-App-Key",
                    "maritime.gateway.security.hmac.headers.timestamp=My-Timestamp",
                    "maritime.gateway.security.hmac.headers.nonce=My-Nonce",
                    "maritime.gateway.security.hmac.headers.body-digest=My-Digest",
                    "maritime.gateway.security.hmac.headers.signature=My-Signature",
                    // credentials
                    "maritime.gateway.security.hmac.credentials.source=redis-only",
                    "maritime.gateway.security.hmac.credentials.redis-key-prefix=custom:app:auth:",
                    // credential fields
                    "maritime.gateway.security.hmac.credentials.fields.app-secret=secret",
                    "maritime.gateway.security.hmac.credentials.fields.app-code=code",
                    "maritime.gateway.security.hmac.credentials.fields.app-id=id",
                    "maritime.gateway.security.hmac.credentials.fields.tenant-id=tid",
                    "maritime.gateway.security.hmac.credentials.fields.tenant-code=tcode",
                    "maritime.gateway.security.hmac.credentials.fields.permissions=perms",
                    "maritime.gateway.security.hmac.credentials.fields.enabled=active",
                    // fallback apps
                    "maritime.gateway.security.hmac.credentials.apps[0].app-key=demo-key",
                    "maritime.gateway.security.hmac.credentials.apps[0].app-secret=demo-secret",
                    "maritime.gateway.security.hmac.credentials.apps[0].app-code=demo",
                    "maritime.gateway.security.hmac.credentials.apps[0].enabled=true"
            ).run(ctx -> {
                GatewaySecurityProperties.Hmac hmac = ctx.getBean(GatewaySecurityProperties.class).getHmac();

                assertThat(hmac.isEnabled()).isTrue();
                assertThat(hmac.getTimestampTolerance()).isEqualTo(Duration.ofMinutes(10));
                assertThat(hmac.getMinNonceLength()).isEqualTo(32);
                assertThat(hmac.getNonceKeyPrefix()).isEqualTo("custom:hmac:nonce:");
                assertThat(hmac.getNonceTtl()).isEqualTo(Duration.ofMinutes(10));

                GatewaySecurityProperties.Headers h = hmac.getHeaders();
                assertThat(h.getAppKey()).isEqualTo("My-App-Key");
                assertThat(h.getTimestamp()).isEqualTo("My-Timestamp");
                assertThat(h.getNonce()).isEqualTo("My-Nonce");
                assertThat(h.getBodyDigest()).isEqualTo("My-Digest");
                assertThat(h.getSignature()).isEqualTo("My-Signature");

                GatewaySecurityProperties.Credentials c = hmac.getCredentials();
                assertThat(c.getSource()).isEqualTo("redis-only");
                assertThat(c.getRedisKeyPrefix()).isEqualTo("custom:app:auth:");

                GatewaySecurityProperties.CredentialFields f = c.getFields();
                assertThat(f.getAppSecret()).isEqualTo("secret");
                assertThat(f.getAppCode()).isEqualTo("code");
                assertThat(f.getAppId()).isEqualTo("id");
                assertThat(f.getTenantId()).isEqualTo("tid");
                assertThat(f.getTenantCode()).isEqualTo("tcode");
                assertThat(f.getPermissions()).isEqualTo("perms");
                assertThat(f.getEnabled()).isEqualTo("active");

                assertThat(c.getApps()).hasSize(1);
                GatewaySecurityProperties.ConfigApp app = c.getApps().get(0);
                assertThat(app.getAppKey()).isEqualTo("demo-key");
                assertThat(app.getAppSecret()).isEqualTo("demo-secret");
                assertThat(app.getAppCode()).isEqualTo("demo");
                assertThat(app.isEnabled()).isTrue();
            });
        }

        @Test
        @DisplayName("binds route policies with kebab-case auth modes")
        void bindsRoutePolicies() {
            runner.withPropertyValues(
                    "maritime.gateway.security.routes[0].id=app-api",
                    "maritime.gateway.security.routes[0].paths[0]=/api/**",
                    "maritime.gateway.security.routes[0].auth-mode=jwt",
                    "maritime.gateway.security.routes[1].id=system-api",
                    "maritime.gateway.security.routes[1].paths[0]=/openapi/**",
                    "maritime.gateway.security.routes[1].auth-mode=hmac",
                    "maritime.gateway.security.routes[2].id=dual-api",
                    "maritime.gateway.security.routes[2].paths[0]=/dual/**",
                    "maritime.gateway.security.routes[2].auth-mode=jwt-or-hmac"
            ).run(ctx -> {
                var routes = ctx.getBean(GatewaySecurityProperties.class).getRoutes();
                assertThat(routes).hasSize(3);

                assertThat(routes.get(0).getId()).isEqualTo("app-api");
                assertThat(routes.get(0).getPaths()).containsExactly("/api/**");
                assertThat(routes.get(0).getAuthMode()).isEqualTo(AuthMode.JWT);

                assertThat(routes.get(1).getId()).isEqualTo("system-api");
                assertThat(routes.get(1).getPaths()).containsExactly("/openapi/**");
                assertThat(routes.get(1).getAuthMode()).isEqualTo(AuthMode.HMAC);

                assertThat(routes.get(2).getId()).isEqualTo("dual-api");
                assertThat(routes.get(2).getPaths()).containsExactly("/dual/**");
                assertThat(routes.get(2).getAuthMode()).isEqualTo(AuthMode.JWT_OR_HMAC);
            });
        }

        @Test
        @DisplayName("binds jwt-and-hmac auth mode (reserved)")
        void bindsJwtAndHmacAuthMode() {
            runner.withPropertyValues(
                    "maritime.gateway.security.default-auth-mode=jwt-and-hmac"
            ).run(ctx -> {
                var props = ctx.getBean(GatewaySecurityProperties.class);
                assertThat(props.getDefaultAuthMode()).isEqualTo(AuthMode.JWT_AND_HMAC);
            });
        }

        @Test
        @DisplayName("binds auth mode as lowercase enum values")
        void bindsLowercaseAuthMode() {
            runner.withPropertyValues(
                    "maritime.gateway.security.default-auth-mode=none",
                    "maritime.gateway.security.routes[0].id=open",
                    "maritime.gateway.security.routes[0].paths[0]=/**",
                    "maritime.gateway.security.routes[0].auth-mode=jwt-or-hmac"
            ).run(ctx -> {
                var props = ctx.getBean(GatewaySecurityProperties.class);
                assertThat(props.getDefaultAuthMode()).isEqualTo(AuthMode.NONE);
                assertThat(props.getRoutes().get(0).getAuthMode()).isEqualTo(AuthMode.JWT_OR_HMAC);
            });
        }
    }

    @Nested
    @DisplayName("Validation logic")
    class ValidationLogic {

        @Test
        @DisplayName("JWT enabled without secret causes failure")
        void jwtEnabledWithoutSecretFails() {
            var props = new GatewaySecurityProperties();
            props.getJwt().setEnabled(true);

            assertThatThrownBy(props::afterPropertiesSet)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("jwt.secret must not be blank");
        }

        @Test
        @DisplayName("JWT enabled with blank secret causes failure")
        void jwtEnabledWithBlankSecretFails() {
            var props = new GatewaySecurityProperties();
            props.getJwt().setEnabled(true);
            props.getJwt().setSecret("   ");

            assertThatThrownBy(props::afterPropertiesSet)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("jwt.secret must not be blank");
        }

        @Test
        @DisplayName("JWT enabled with blank issuer causes failure")
        void jwtEnabledWithBlankIssuerFails() {
            var props = new GatewaySecurityProperties();
            props.getJwt().setEnabled(true);
            props.getJwt().setSecret("some-secret");
            props.getJwt().setIssuer("   ");

            assertThatThrownBy(props::afterPropertiesSet)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("jwt.issuer must not be blank");
        }

        @Test
        @DisplayName("HMAC enabled with negative timestamp tolerance causes failure")
        void hmacEnabledWithNegativeTimestampFails() {
            var props = new GatewaySecurityProperties();
            props.getHmac().setEnabled(true);
            props.getHmac().setTimestampTolerance(Duration.ofMinutes(-1));

            assertThatThrownBy(props::afterPropertiesSet)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("timestamp-tolerance");
        }

        @Test
        @DisplayName("HMAC enabled with negative nonce ttl causes failure")
        void hmacEnabledWithNegativeNonceTtlFails() {
            var props = new GatewaySecurityProperties();
            props.getHmac().setEnabled(true);
            props.getHmac().setNonceTtl(Duration.ofMinutes(-1));

            assertThatThrownBy(props::afterPropertiesSet)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nonce-ttl");
        }

        @Test
        @DisplayName("JWT disabled passes validation regardless of other fields")
        void jwtDisabledPassesValidation() {
            var props = new GatewaySecurityProperties();
            // JWT disabled by default, secret is null
            props.afterPropertiesSet(); // should not throw
        }

        @Test
        @DisplayName("HMAC disabled passes validation regardless of other fields")
        void hmacDisabledPassesValidation() {
            var props = new GatewaySecurityProperties();
            // HMAC disabled by default
            props.afterPropertiesSet(); // should not throw
        }
    }

    @Nested
    @DisplayName("Bean validation constraints")
    class BeanValidationConstraints {

        static Validator validator;

        @BeforeAll
        static void setUpValidator() {
            try (var factory = Validation.buildDefaultValidatorFactory()) {
                validator = factory.getValidator();
            }
        }

        @Test
        @DisplayName("RoutePolicy without id fails @NotBlank")
        void routePolicyWithoutIdFails() {
            var route = new GatewaySecurityProperties.RoutePolicy();
            route.setPaths(java.util.List.of("/api/**"));
            route.setAuthMode(AuthMode.JWT);

            var violations = validator.validate(route);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("id"));
        }

        @Test
        @DisplayName("RoutePolicy without paths fails @NotEmpty")
        void routePolicyWithoutPathsFails() {
            var route = new GatewaySecurityProperties.RoutePolicy();
            route.setId("empty-route");
            route.setAuthMode(AuthMode.JWT);

            var violations = validator.validate(route);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("paths"));
        }

        @Test
        @DisplayName("Valid RoutePolicy passes validation")
        void validRoutePolicyPasses() {
            var route = new GatewaySecurityProperties.RoutePolicy();
            route.setId("valid-route");
            route.setPaths(java.util.List.of("/api/**"));
            route.setAuthMode(AuthMode.JWT);

            var violations = validator.validate(route);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("ConfigApp without appKey fails @NotBlank")
        void configAppWithoutAppKeyFails() {
            var app = new GatewaySecurityProperties.ConfigApp();
            app.setAppSecret("secret");
            app.setAppCode("code");

            var violations = validator.validate(app);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("appKey"));
        }

        @Test
        @DisplayName("ConfigApp without appSecret fails @NotBlank")
        void configAppWithoutAppSecretFails() {
            var app = new GatewaySecurityProperties.ConfigApp();
            app.setAppKey("key");
            app.setAppCode("code");

            var violations = validator.validate(app);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("appSecret"));
        }

        @Test
        @DisplayName("ConfigApp without appCode fails @NotBlank")
        void configAppWithoutAppCodeFails() {
            var app = new GatewaySecurityProperties.ConfigApp();
            app.setAppKey("key");
            app.setAppSecret("secret");

            var violations = validator.validate(app);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("appCode"));
        }

        @Test
        @DisplayName("Valid ConfigApp passes validation")
        void validConfigAppPasses() {
            var app = new GatewaySecurityProperties.ConfigApp();
            app.setAppKey("key");
            app.setAppSecret("secret");
            app.setAppCode("code");

            var violations = validator.validate(app);
            assertThat(violations).isEmpty();
        }
    }
}
