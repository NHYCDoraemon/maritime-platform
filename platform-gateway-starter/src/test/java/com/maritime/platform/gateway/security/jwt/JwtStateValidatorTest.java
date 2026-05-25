package com.maritime.platform.gateway.security.jwt;

import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@DisplayName("JwtStateValidator tests")
@ExtendWith(MockitoExtension.class)
class JwtStateValidatorTest {

    @Mock
    private ReactiveRedisOperations<String, String> redisOps;

    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    private GatewaySecurityProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GatewaySecurityProperties();
        properties.getJwt().setEnabled(true);
        properties.getJwt().setSecret("my-test-secret-key-minimum-256-bits-long!!");
        properties.getJwt().setIssuer("maritime-platform");
        lenient().when(redisOps.opsForValue()).thenReturn(valueOps);
    }

    private JwtStateValidator validator() {
        return new JwtStateValidator(redisOps, properties);
    }

    /** Disable all but session check for isolated testing. */
    private void isolateSessionCheck() {
        properties.getJwt().getValidation().setCheckBlacklist(false);
        properties.getJwt().getValidation().setCheckUserEnabled(false);
    }

    /** Disable user-enabled check for blacklist-focused testing. */
    private void isolateBlacklistCheck() {
        properties.getJwt().getValidation().setCheckUserEnabled(false);
    }

    // ---------- session validation ----------

    @Nested
    @DisplayName("Session validation")
    class SessionValidation {

        @Test
        @DisplayName("session exists in Redis passes validation")
        void sessionExistsPasses() {
            isolateSessionCheck();
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(validator().validate("session-abc", "jti-123", "user-123"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("session not found in Redis returns SESSION_EXPIRED")
        void sessionNotFoundReturnsSessionExpired() {
            isolateSessionCheck();
            when(redisOps.hasKey("iam:session:session-expired"))
                    .thenReturn(Mono.just(false));

            StepVerifier.create(validator().validate("session-expired", "jti-123", "user-123"))
                    .verifyErrorSatisfies(e -> {
                        assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.SESSION_EXPIRED);
                    });
        }

        @Test
        @DisplayName("requireSession disabled skips session check")
        void sessionCheckDisabledSkips() {
            properties.getJwt().getValidation().setRequireSession(false);
            properties.getJwt().getValidation().setCheckBlacklist(false);
            properties.getJwt().getValidation().setCheckUserEnabled(false);

            StepVerifier.create(validator().validate("any-session", "jti-123", "user-123"))
                    .verifyComplete();
        }
    }

    // ---------- blacklist validation ----------

    @Nested
    @DisplayName("Blacklist validation")
    class BlacklistValidation {

        @Test
        @DisplayName("token not in blacklist passes validation")
        void tokenNotBlacklistedPasses() {
            isolateBlacklistCheck();
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));
            when(redisOps.hasKey("iam:token:blacklist:jti-123"))
                    .thenReturn(Mono.just(false));

            StepVerifier.create(validator().validate("session-abc", "jti-123", "user-123"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("token in blacklist returns TOKEN_BLACKLISTED")
        void tokenBlacklistedReturnsTokenBlacklisted() {
            isolateBlacklistCheck();
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));
            when(redisOps.hasKey("iam:token:blacklist:jti-blacklisted"))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(validator().validate("session-abc", "jti-blacklisted", "user-123"))
                    .verifyErrorSatisfies(e -> {
                        assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.TOKEN_BLACKLISTED);
                    });
        }

        @Test
        @DisplayName("checkBlacklist disabled skips blacklist check")
        void blacklistCheckDisabledSkips() {
            isolateBlacklistCheck();
            properties.getJwt().getValidation().setCheckBlacklist(false);
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(validator().validate("session-abc", "jti-blacklisted", "user-123"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("null jti with blacklist enabled skips check gracefully")
        void nullJtiSkipsBlacklistCheck() {
            isolateBlacklistCheck();
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(validator().validate("session-abc", null, "user-123"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("empty jti with blacklist enabled skips check gracefully")
        void emptyJtiSkipsBlacklistCheck() {
            isolateBlacklistCheck();
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(validator().validate("session-abc", "", "user-123"))
                    .verifyComplete();
        }
    }

    // ---------- user enabled validation ----------

    @Nested
    @DisplayName("User enabled validation")
    class UserEnabledValidation {

        @Test
        @DisplayName("user enabled in Redis passes validation")
        void userEnabledPasses() {
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));
            when(redisOps.hasKey("iam:token:blacklist:jti-123"))
                    .thenReturn(Mono.just(false));
            when(valueOps.get("iam:user:enabled:user-123"))
                    .thenReturn(Mono.just("1"));

            StepVerifier.create(validator().validate("session-abc", "jti-123", "user-123"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("user disabled in Redis returns USER_DISABLED")
        void userDisabledReturnsUserDisabled() {
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));
            when(redisOps.hasKey("iam:token:blacklist:jti-123"))
                    .thenReturn(Mono.just(false));
            when(valueOps.get("iam:user:enabled:user-disabled"))
                    .thenReturn(Mono.just("0"));

            StepVerifier.create(validator().validate("session-abc", "jti-123", "user-disabled"))
                    .verifyErrorSatisfies(e -> {
                        assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.USER_DISABLED);
                    });
        }

        @Test
        @DisplayName("checkUserEnabled disabled skips user enabled check")
        void userEnabledCheckDisabledSkips() {
            properties.getJwt().getValidation().setCheckUserEnabled(false);
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));
            when(redisOps.hasKey("iam:token:blacklist:jti-123"))
                    .thenReturn(Mono.just(false));

            StepVerifier.create(validator().validate("session-abc", "jti-123", "user-disabled"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("user enabled key not found in Redis passes validation")
        void userEnabledKeyNotFoundPasses() {
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));
            when(redisOps.hasKey("iam:token:blacklist:jti-123"))
                    .thenReturn(Mono.just(false));
            when(valueOps.get("iam:user:enabled:user-123"))
                    .thenReturn(Mono.empty());

            StepVerifier.create(validator().validate("session-abc", "jti-123", "user-123"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("custom user-enabled-disabled-value is respected")
        void customDisabledValueRespected() {
            properties.getJwt().getValidation().setUserEnabledDisabledValue("-1");
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));
            when(redisOps.hasKey("iam:token:blacklist:jti-123"))
                    .thenReturn(Mono.just(false));
            when(valueOps.get("iam:user:enabled:user-123"))
                    .thenReturn(Mono.just("-1"));

            StepVerifier.create(validator().validate("session-abc", "jti-123", "user-123"))
                    .verifyErrorSatisfies(e -> {
                        assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.USER_DISABLED);
                    });
        }
    }

    // ---------- custom Redis key prefixes ----------

    @Nested
    @DisplayName("Custom Redis key prefixes")
    class CustomKeyPrefixes {

        @Test
        @DisplayName("custom session prefix is used")
        void customSessionPrefixUsed() {
            isolateSessionCheck();
            properties.getJwt().getRedisKeys().setSessionPrefix("custom:session:");
            when(redisOps.hasKey("custom:session:session-abc"))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(validator().validate("session-abc", "jti-123", "user-123"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("custom blacklist prefix is used")
        void customBlacklistPrefixUsed() {
            isolateBlacklistCheck();
            properties.getJwt().getRedisKeys().setBlacklistPrefix("custom:blacklist:");
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));
            when(redisOps.hasKey("custom:blacklist:jti-123"))
                    .thenReturn(Mono.just(false));

            StepVerifier.create(validator().validate("session-abc", "jti-123", "user-123"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("custom user enabled prefix is used")
        void customUserEnabledPrefixUsed() {
            properties.getJwt().getRedisKeys().setUserEnabledPrefix("custom:user:enabled:");
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));
            when(redisOps.hasKey("iam:token:blacklist:jti-123"))
                    .thenReturn(Mono.just(false));
            when(valueOps.get("custom:user:enabled:user-123"))
                    .thenReturn(Mono.just("1"));

            StepVerifier.create(validator().validate("session-abc", "jti-123", "user-123"))
                    .verifyComplete();
        }
    }

    // ---------- combined validation ----------

    @Nested
    @DisplayName("Combined validation")
    class CombinedValidation {

        @Test
        @DisplayName("all checks pass with valid state")
        void allChecksPass() {
            when(redisOps.hasKey("iam:session:session-abc"))
                    .thenReturn(Mono.just(true));
            when(redisOps.hasKey("iam:token:blacklist:jti-123"))
                    .thenReturn(Mono.just(false));
            when(valueOps.get("iam:user:enabled:user-123"))
                    .thenReturn(Mono.just("1"));

            StepVerifier.create(validator().validate("session-abc", "jti-123", "user-123"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("all checks disabled passes immediately")
        void allChecksDisabledPasses() {
            properties.getJwt().getValidation().setRequireSession(false);
            properties.getJwt().getValidation().setCheckBlacklist(false);
            properties.getJwt().getValidation().setCheckUserEnabled(false);

            StepVerifier.create(validator().validate("any-session", "any-jti", "any-user"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("session check fails first before other checks")
        void sessionCheckFailsFirst() {
            isolateSessionCheck();
            when(redisOps.hasKey("iam:session:session-expired"))
                    .thenReturn(Mono.just(false));

            StepVerifier.create(validator().validate("session-expired", "jti-123", "user-123"))
                    .verifyErrorSatisfies(e -> {
                        assertThat(e)
                                .isInstanceOf(JwtAuthenticationException.class)
                                .extracting("errorCode").isEqualTo(GatewayAuthErrorCode.SESSION_EXPIRED);
                    });
        }
    }
}
