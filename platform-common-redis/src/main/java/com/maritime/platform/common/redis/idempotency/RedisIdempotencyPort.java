package com.maritime.platform.common.redis.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed {@link IdempotencyPort} implementation. Serializes the result
 * record as JSON and stores it with an absolute TTL.
 */
public class RedisIdempotencyPort implements IdempotencyPort {

    private static final String STATE_PROCESSING = "PROCESSING";
    private static final String STATE_SUCCEEDED = "SUCCEEDED";
    private static final String STATE_FAILED = "FAILED";

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final String keyPrefix;

    public RedisIdempotencyPort(StringRedisTemplate redis, ObjectMapper json, String keyPrefix) {
        this.redis = redis;
        this.json = json;
        this.keyPrefix = keyPrefix;
    }

    private String key(String tenantId, String idempotencyKey) {
        return keyPrefix + ":" + tenantId + ":" + idempotencyKey;
    }

    @Override
    public Optional<IdempotencyResult> findResult(String tenantId, String idempotencyKey) {
        String v = redis.opsForValue().get(key(tenantId, idempotencyKey));
        if (v == null) {
            return Optional.empty();
        }
        return readResult(v);
    }

    @Override
    public BeginProcessingResult beginProcessing(
            String tenantId,
            String idempotencyKey,
            String operationType,
            Duration processingTtl) {
        if (tenantId == null || tenantId.isBlank()
                || idempotencyKey == null || idempotencyKey.isBlank()) {
            return BeginProcessingResult.conflict();
        }
        Duration ttl = requirePositiveTtl(processingTtl);
        String redisKey = key(tenantId, idempotencyKey);
        StoredRecord processing = StoredRecord.processing(operationType);
        try {
            Boolean stored = redis.opsForValue().setIfAbsent(
                    redisKey,
                    json.writeValueAsString(processing),
                    ttl.toMillis(),
                    TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(stored)) {
                return BeginProcessingResult.started();
            }
            String existing = redis.opsForValue().get(redisKey);
            if (existing == null) {
                return BeginProcessingResult.inProgress();
            }
            return classifyExisting(existing, operationType);
        } catch (Exception e) {
            throw new IdempotencyStoreException(
                    "Failed to claim idempotency key " + redisKey, e);
        }
    }

    @Override
    public boolean recordResult(IdempotencyRecord record, Duration ttl) {
        try {
            String v = json.writeValueAsString(StoredRecord.succeeded(record));
            Boolean set = redis.opsForValue().setIfAbsent(
                    key(record.tenantId(), record.idempotencyKey()),
                    v,
                    requirePositiveTtl(ttl).toMillis(),
                    TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(set);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void completeProcessing(IdempotencyRecord record, Duration ttl) {
        try {
            redis.opsForValue().set(
                    key(record.tenantId(), record.idempotencyKey()),
                    json.writeValueAsString(StoredRecord.succeeded(record)),
                    requirePositiveTtl(ttl).toMillis(),
                    TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IdempotencyStoreException(
                    "Failed to complete idempotency key "
                            + key(record.tenantId(), record.idempotencyKey()), e);
        }
    }

    @Override
    public void clearProcessing(String tenantId, String idempotencyKey) {
        redis.delete(key(tenantId, idempotencyKey));
    }

    @Override
    public void failProcessing(
            String tenantId,
            String idempotencyKey,
            String operationType,
            String errorCode,
            String message,
            Duration ttl) {
        try {
            redis.opsForValue().set(
                    key(tenantId, idempotencyKey),
                    json.writeValueAsString(StoredRecord.failed(operationType, errorCode, message)),
                    requirePositiveTtl(ttl).toMillis(),
                    TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IdempotencyStoreException(
                    "Failed to record idempotency failure " + key(tenantId, idempotencyKey), e);
        }
    }

    @Override
    public boolean isProcessed(String tenantId, String idempotencyKey) {
        Boolean exists = redis.hasKey(key(tenantId, idempotencyKey));
        return Boolean.TRUE.equals(exists);
    }

    private BeginProcessingResult classifyExisting(String value, String operationType) {
        try {
            StoredRecord stored = json.readValue(value, StoredRecord.class);
            if (!sameOperation(stored.operationType(), operationType)) {
                return BeginProcessingResult.conflict();
            }
            return switch (stored.state()) {
                case STATE_PROCESSING -> BeginProcessingResult.inProgress();
                case STATE_SUCCEEDED -> BeginProcessingResult.replay(
                        new IdempotencyResult(stored.resultJson(), stored.executedAt(), stored.operationType()));
                case STATE_FAILED -> BeginProcessingResult.failed(
                        new IdempotencyFailure(
                                stored.errorCode(),
                                stored.message(),
                                stored.executedAt(),
                                stored.operationType()));
                default -> BeginProcessingResult.conflict();
            };
        } catch (Exception ignored) {
            return readLegacyResult(value)
                    .filter(result -> sameOperation(result.operationType(), operationType))
                    .map(BeginProcessingResult::replay)
                    .orElseGet(BeginProcessingResult::conflict);
        }
    }

    private Optional<IdempotencyResult> readResult(String value) {
        try {
            StoredRecord stored = json.readValue(value, StoredRecord.class);
            if (!STATE_SUCCEEDED.equals(stored.state())) {
                return Optional.empty();
            }
            return Optional.of(new IdempotencyResult(
                    stored.resultJson(), stored.executedAt(), stored.operationType()));
        } catch (Exception ignored) {
            return readLegacyResult(value);
        }
    }

    private Optional<IdempotencyResult> readLegacyResult(String value) {
        try {
            return Optional.of(json.readValue(value, IdempotencyResult.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static boolean sameOperation(String existing, String requested) {
        String existingValue = existing == null ? "" : existing;
        String requestedValue = requested == null ? "" : requested;
        return existingValue.equals(requestedValue);
    }

    private static Duration requirePositiveTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        return ttl;
    }

    private record StoredRecord(
            String state,
            String resultJson,
            Instant executedAt,
            String operationType,
            String errorCode,
            String message
    ) {
        static StoredRecord processing(String operationType) {
            return new StoredRecord(STATE_PROCESSING, "", Instant.now(), operationType, "", "");
        }

        static StoredRecord succeeded(IdempotencyRecord record) {
            return new StoredRecord(
                    STATE_SUCCEEDED,
                    record.resultJson(),
                    record.executedAt(),
                    record.operationType(),
                    "",
                    "");
        }

        static StoredRecord failed(String operationType, String errorCode, String message) {
            return new StoredRecord(STATE_FAILED, "", Instant.now(), operationType, errorCode, message);
        }
    }
}
