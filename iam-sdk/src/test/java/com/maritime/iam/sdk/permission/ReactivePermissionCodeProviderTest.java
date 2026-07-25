package com.maritime.iam.sdk.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

class ReactivePermissionCodeProviderTest {

    private static final String SYSTEM_CODE = "TODO";
    private static final String USER_ID = "user-1";
    private static final String ORG_CODE = "org-1";
    private static final String CACHE_KEY =
            "iam:perms:TODO:user-1:org-1";

    private ReactiveIamPermissionClient client;
    private ReactiveValueOperations<String, String> values;
    private ReactivePermissionCodeProvider provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        client = mock(ReactiveIamPermissionClient.class);
        ReactiveStringRedisTemplate redis =
                mock(ReactiveStringRedisTemplate.class);
        values = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        provider = new ReactivePermissionCodeProvider(
                client,
                redis,
                SYSTEM_CODE,
                Duration.ofMinutes(30),
                Duration.ofMinutes(2),
                Duration.ZERO,
                false,
                16 * 1024);
    }

    @Test
    void matchingVersionReturnsCachedCodes() {
        when(client.getPermissionVersion(
                SYSTEM_CODE, USER_ID, ORG_CODE))
                .thenReturn(Mono.just("1.7"));
        when(values.get(CACHE_KEY))
                .thenReturn(Mono.just(
                        "ver:1.7|todo:read,todo:write"));

        PermissionCodeSnapshot snapshot = provider
                .getPermissionCodes(USER_ID, ORG_CODE)
                .block();

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.codes()).containsExactly(
                "todo:read", "todo:write");
        verify(client, never()).getPermissionCodes(
                anyString(), anyString(), anyString());
    }

    @Test
    void versionMismatchFetchesAndCachesFreshCodes() {
        when(client.getPermissionVersion(
                SYSTEM_CODE, USER_ID, ORG_CODE))
                .thenReturn(Mono.just("1.8"));
        when(values.get(CACHE_KEY))
                .thenReturn(Mono.just("ver:1.7|todo:read"));
        when(client.getPermissionCodes(
                SYSTEM_CODE, USER_ID, ORG_CODE))
                .thenReturn(Mono.just(
                        "todo:read,todo:process,todo:read"));
        when(values.set(
                CACHE_KEY,
                "ver:1.8|todo:read,todo:process",
                Duration.ofMinutes(30)))
                .thenReturn(Mono.just(true));

        PermissionCodeSnapshot snapshot = provider
                .getPermissionCodes(USER_ID, ORG_CODE)
                .block();

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.version()).isEqualTo("1.8");
        assertThat(snapshot.codes()).containsExactly(
                "todo:read", "todo:process");
        verify(values).set(
                CACHE_KEY,
                "ver:1.8|todo:read,todo:process",
                Duration.ofMinutes(30));
    }

    @Test
    void emptyPermissionsAreNegativeCached() {
        when(client.getPermissionVersion(
                SYSTEM_CODE, USER_ID, ORG_CODE))
                .thenReturn(Mono.just("2.9"));
        when(values.get(CACHE_KEY)).thenReturn(Mono.empty());
        when(client.getPermissionCodes(
                SYSTEM_CODE, USER_ID, ORG_CODE))
                .thenReturn(Mono.just(""));
        when(values.set(
                CACHE_KEY, "ver:2.9|", Duration.ofMinutes(2)))
                .thenReturn(Mono.just(true));

        PermissionCodeSnapshot snapshot = provider
                .getPermissionCodes(USER_ID, ORG_CODE)
                .block();

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.codes()).isEmpty();
        verify(values).set(
                CACHE_KEY, "ver:2.9|", Duration.ofMinutes(2));
    }

    @Test
    void iamFailureIsClosedByDefault() {
        when(client.getPermissionVersion(
                SYSTEM_CODE, USER_ID, ORG_CODE))
                .thenReturn(Mono.error(
                        new IllegalStateException("IAM down")));

        assertThatThrownBy(() -> provider
                .getPermissionCodes(USER_ID, ORG_CODE)
                .block())
                .isInstanceOf(IamPermissionUnavailableException.class)
                .hasMessageContaining(
                        "Cannot verify IAM permission version");
        verify(values, never()).set(
                anyString(), anyString(), any(Duration.class));
    }

    @Test
    void redisReadFailureQueriesIamAndContinuesWithoutCache() {
        when(client.getPermissionVersion(
                SYSTEM_CODE, USER_ID, ORG_CODE))
                .thenReturn(Mono.just("3.4"));
        when(values.get(CACHE_KEY))
                .thenReturn(Mono.error(
                        new IllegalStateException("Redis down")));
        when(client.getPermissionCodes(
                SYSTEM_CODE, USER_ID, ORG_CODE))
                .thenReturn(Mono.just("todo:read"));
        when(values.set(
                CACHE_KEY,
                "ver:3.4|todo:read",
                Duration.ofMinutes(30)))
                .thenReturn(Mono.error(
                        new IllegalStateException("Redis down")));

        PermissionCodeSnapshot snapshot = provider
                .getPermissionCodes(USER_ID, ORG_CODE)
                .block();

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.codes()).containsExactly("todo:read");
    }

    @Test
    void olderIamFallsBackToTtlOnlyCache() {
        when(client.getPermissionVersion(
                SYSTEM_CODE, USER_ID, ORG_CODE))
                .thenReturn(Mono.error(
                        new PermissionVersionEndpointUnsupportedException()));
        when(values.get(CACHE_KEY))
                .thenReturn(Mono.just("ver:0.0|todo:read"));

        PermissionCodeSnapshot first = provider
                .getPermissionCodes(USER_ID, ORG_CODE)
                .block();
        PermissionCodeSnapshot second = provider
                .getPermissionCodes(USER_ID, ORG_CODE)
                .block();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first.codes()).containsExactly("todo:read");
        verify(client).getPermissionVersion(
                SYSTEM_CODE, USER_ID, ORG_CODE);
    }

    @Test
    void explicitFailOpenUsesPreviouslyCachedGrant() {
        provider = provider(true);
        when(client.getPermissionVersion(
                SYSTEM_CODE, USER_ID, ORG_CODE))
                .thenReturn(Mono.error(
                        new IllegalStateException("IAM down")));
        when(values.get(CACHE_KEY))
                .thenReturn(Mono.just(
                        "ver:2.3|todo:read,todo:process"));

        PermissionCodeSnapshot snapshot = provider
                .getPermissionCodes(USER_ID, ORG_CODE)
                .block();

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.version()).isEqualTo("2.3");
        assertThat(snapshot.codes()).containsExactly(
                "todo:read", "todo:process");
    }

    @Test
    void rejectsPermissionHeaderAboveConfiguredLimit() {
        provider = new ReactivePermissionCodeProvider(
                client,
                redis(),
                SYSTEM_CODE,
                Duration.ofMinutes(30),
                Duration.ofMinutes(2),
                Duration.ZERO,
                false,
                4);
        when(client.getPermissionVersion(
                SYSTEM_CODE, USER_ID, ORG_CODE))
                .thenReturn(Mono.just("1.0"));
        when(values.get(CACHE_KEY)).thenReturn(Mono.empty());
        when(client.getPermissionCodes(
                SYSTEM_CODE, USER_ID, ORG_CODE))
                .thenReturn(Mono.just("todo:read"));

        assertThatThrownBy(() -> provider
                .getPermissionCodes(USER_ID, ORG_CODE)
                .block())
                .isInstanceOf(IamPermissionUnavailableException.class)
                .hasMessageContaining(
                        "permission header exceeds 4 bytes");
    }

    private ReactivePermissionCodeProvider provider(
            boolean failOpen) {
        return new ReactivePermissionCodeProvider(
                client,
                redis(),
                SYSTEM_CODE,
                Duration.ofMinutes(30),
                Duration.ofMinutes(2),
                Duration.ZERO,
                failOpen,
                16 * 1024);
    }

    private ReactiveStringRedisTemplate redis() {
        ReactiveStringRedisTemplate redis =
                mock(ReactiveStringRedisTemplate.class);
        when(redis.opsForValue()).thenReturn(values);
        return redis;
    }
}
