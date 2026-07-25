package com.maritime.iam.sdk.permission;

import com.maritime.iam.sdk.cache.IamSdkCacheKeys;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

/**
 * Business-side L2 permission-code cache with IAM version reconciliation.
 *
 * <p>Correctness does not depend solely on MQ delivery. A short-lived local
 * version observation is reconciled with IAM's authoritative version; a
 * mismatch forces a fresh effective-permission query. MQ invalidation remains
 * the low-latency path, while version reconciliation covers process downtime,
 * cross-vhost deployments and lost events.</p>
 */
public class ReactivePermissionCodeProvider {

    private static final Logger LOG = LoggerFactory.getLogger(
            ReactivePermissionCodeProvider.class);

    private static final String CACHE_PREFIX = "ver:";
    private static final char CACHE_SEPARATOR = '|';

    private final ReactiveIamPermissionClient client;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final String systemCode;
    private final Duration cacheTtl;
    private final Duration emptyCacheTtl;
    private final Duration versionCheckInterval;
    private final boolean failOpen;
    private final int maxHeaderBytes;
    private final AtomicReference<VersionObservation> versionObservation =
            new AtomicReference<>();
    private final AtomicBoolean versionEndpointUnsupported =
            new AtomicBoolean();
    private final Map<String, Mono<PermissionCodeSnapshot>> inFlight =
            new ConcurrentHashMap<>();

    public ReactivePermissionCodeProvider(
            ReactiveIamPermissionClient client,
            ReactiveStringRedisTemplate redisTemplate,
            String systemCode,
            Duration cacheTtl,
            Duration emptyCacheTtl,
            Duration versionCheckInterval,
            boolean failOpen,
            int maxHeaderBytes) {
        this.client = client;
        this.redisTemplate = redisTemplate;
        this.systemCode = requireText(systemCode, "systemCode");
        this.cacheTtl = requirePositive(cacheTtl, "cacheTtl");
        this.emptyCacheTtl =
                requirePositive(emptyCacheTtl, "emptyCacheTtl");
        this.versionCheckInterval = requireNonNegative(
                versionCheckInterval, "versionCheckInterval");
        this.failOpen = failOpen;
        if (maxHeaderBytes < 1) {
            throw new IllegalArgumentException(
                    "maxHeaderBytes must be positive");
        }
        this.maxHeaderBytes = maxHeaderBytes;
    }

    /**
     * Resolve current permission codes for a trusted IAM identity.
     */
    public Mono<PermissionCodeSnapshot> getPermissionCodes(
            String userId, String activeOrgCode) {
        requireText(userId, "userId");
        String key = IamSdkCacheKeys.permissionCodes(
                systemCode, userId, activeOrgCode);
        return currentVersion(userId, activeOrgCode)
                .flatMap(version -> readOrFetch(
                        key, userId, activeOrgCode, version))
                .onErrorResume(error -> handleFailure(key, error));
    }

    /**
     * Force the next request to reconcile the version with IAM.
     */
    public void invalidateVersionObservation() {
        versionObservation.set(null);
    }

    private Mono<PermissionCodeSnapshot> readOrFetch(
            String key, String userId,
            String activeOrgCode, String version) {
        return redisTemplate.opsForValue().get(key)
                .onErrorResume(error -> {
                    LOG.warn("IAM SDK cache read failed for system={}, "
                                    + "user={}; querying IAM directly",
                            systemCode, userId, error);
                    return Mono.empty();
                })
                .flatMap(cached -> parseCached(cached, version))
                .switchIfEmpty(Mono.defer(() -> singleFlightFetch(
                        key, userId, activeOrgCode, version)));
    }

    private Mono<PermissionCodeSnapshot> parseCached(
            String cached, String expectedVersion) {
        if (cached == null || !cached.startsWith(CACHE_PREFIX)) {
            return Mono.empty();
        }
        int separator = cached.indexOf(CACHE_SEPARATOR);
        if (separator < CACHE_PREFIX.length()) {
            return Mono.empty();
        }
        String cachedVersion = cached.substring(
                CACHE_PREFIX.length(), separator);
        if (!cachedVersion.equals(expectedVersion)) {
            return Mono.empty();
        }
        return Mono.just(validateSize(
                PermissionCodeSnapshot.parse(
                        cachedVersion,
                        cached.substring(separator + 1))));
    }

    private Mono<PermissionCodeSnapshot> singleFlightFetch(
            String key, String userId,
            String activeOrgCode, String version) {
        return inFlight.computeIfAbsent(key, ignored ->
                fetchAndCache(key, userId, activeOrgCode, version)
                        .cache()
                        .doFinally(signal -> inFlight.remove(key)));
    }

    private Mono<PermissionCodeSnapshot> fetchAndCache(
            String key, String userId,
            String activeOrgCode, String version) {
        return client.getPermissionCodes(
                        systemCode, userId, activeOrgCode)
                .map(codes -> validateSize(
                        PermissionCodeSnapshot.parse(version, codes)))
                .flatMap(snapshot -> {
                    String value = CACHE_PREFIX + version
                            + CACHE_SEPARATOR + snapshot.headerValue();
                    Duration ttl = snapshot.codes().isEmpty()
                            ? emptyCacheTtl : cacheTtl;
                    return redisTemplate.opsForValue()
                            .set(key, value, ttl)
                            .onErrorResume(error -> {
                                LOG.warn("IAM SDK cache write failed for "
                                                + "system={}, user={}",
                                        systemCode, userId, error);
                                return Mono.just(false);
                            })
                            .thenReturn(snapshot);
                })
                .onErrorMap(error -> error
                                instanceof IamPermissionUnavailableException,
                        error -> error)
                .onErrorMap(error -> !(
                                error instanceof IamPermissionUnavailableException),
                        error -> new IamPermissionUnavailableException(
                                "Cannot refresh IAM permission codes", error));
    }

    private Mono<String> currentVersion(
            String userId, String activeOrgCode) {
        if (versionEndpointUnsupported.get()) {
            return Mono.just("0.0");
        }
        VersionObservation observed = versionObservation.get();
        long now = System.nanoTime();
        if (observed != null && observed.validAt(now)) {
            return Mono.just(observed.version());
        }
        return client.getPermissionVersion(
                        systemCode, userId, activeOrgCode)
                .doOnNext(version -> versionObservation.set(
                        new VersionObservation(
                                version,
                                now + versionCheckInterval.toNanos())))
                .onErrorResume(
                        PermissionVersionEndpointUnsupportedException.class,
                        error -> {
                            versionEndpointUnsupported.set(true);
                            LOG.warn("IAM does not expose permission "
                                    + "version; falling back to TTL-only "
                                    + "cache reconciliation for system={}",
                                    systemCode);
                            return Mono.just("0.0");
                        })
                .onErrorMap(error -> error
                                instanceof IamPermissionUnavailableException,
                        error -> error)
                .onErrorMap(error -> !(
                                error instanceof IamPermissionUnavailableException),
                        error -> new IamPermissionUnavailableException(
                                "Cannot verify IAM permission version",
                                error));
    }

    private Mono<PermissionCodeSnapshot> handleFailure(
            String key, Throwable error) {
        if (!failOpen) {
            return Mono.error(error);
        }
        LOG.warn("IAM permission verification failed for system={}; "
                        + "explicit fail-open mode will use cached grants",
                systemCode, error);
        return redisTemplate.opsForValue().get(key)
                .flatMap(this::parseAnyCached)
                .switchIfEmpty(Mono.error(error));
    }

    private Mono<PermissionCodeSnapshot> parseAnyCached(String cached) {
        if (cached == null || !cached.startsWith(CACHE_PREFIX)) {
            return Mono.empty();
        }
        int separator = cached.indexOf(CACHE_SEPARATOR);
        if (separator < CACHE_PREFIX.length()) {
            return Mono.empty();
        }
        String version = cached.substring(
                CACHE_PREFIX.length(), separator);
        return Mono.just(validateSize(
                PermissionCodeSnapshot.parse(
                        version,
                        cached.substring(separator + 1))));
    }

    private PermissionCodeSnapshot validateSize(
            PermissionCodeSnapshot snapshot) {
        if (snapshot.headerBytes() > maxHeaderBytes) {
            throw new IamPermissionUnavailableException(
                    "IAM permission header exceeds "
                            + maxHeaderBytes + " bytes");
        }
        return snapshot;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " required");
        }
        return value;
    }

    private static Duration requirePositive(
            Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    name + " must be positive");
        }
        return value;
    }

    private static Duration requireNonNegative(
            Duration value, String name) {
        if (value == null || value.isNegative()) {
            throw new IllegalArgumentException(
                    name + " must not be negative");
        }
        return value;
    }

    private record VersionObservation(
            String version, long expiresAtNanos) {

        boolean validAt(long nowNanos) {
            return nowNanos < expiresAtNanos;
        }
    }
}
