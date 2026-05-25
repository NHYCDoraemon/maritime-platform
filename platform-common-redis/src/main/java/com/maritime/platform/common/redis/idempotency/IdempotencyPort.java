package com.maritime.platform.common.redis.idempotency;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Idempotency key storage + lookup primitive. Records the outcome of an
 * operation keyed by {@code (tenantId, idempotencyKey)} so that retried
 * requests can short-circuit with the previous result.
 *
 * <p>Key format: {@code <keyPrefix>:<tenantId>:<idempotencyKey>} (prefix configurable
 * per {@code IdempotencyPort} bean, default {@code "platform:idem"}).</p>
 */
public interface IdempotencyPort {

    /**
     * Retrieve a previously recorded result. Returns empty if not found or the
     * stored value could not be deserialized.
     */
    Optional<IdempotencyResult> findResult(String tenantId, String idempotencyKey);

    /**
     * Atomically claims a key before executing a mutating operation.
     *
     * <p>This prevents concurrent duplicate execution while the first caller is
     * still running. The processing claim expires after {@code processingTtl}
     * so crashed callers do not block the key forever.</p>
     */
    BeginProcessingResult beginProcessing(
            String tenantId,
            String idempotencyKey,
            String operationType,
            Duration processingTtl);

    /**
     * Record a result for the given key, iff no value exists yet (atomic set-if-absent).
     * The record expires after {@code ttl}.
     *
     * @return {@code true} if stored; {@code false} if the key already exists or the
     *         serialization/Redis call failed
     */
    boolean recordResult(IdempotencyRecord record, Duration ttl);

    /**
     * Replaces an in-progress claim with the final replayable result.
     */
    void completeProcessing(IdempotencyRecord record, Duration ttl);

    /**
     * Clears an in-progress claim after a caller chooses not to keep a terminal
     * failure record.
     */
    void clearProcessing(String tenantId, String idempotencyKey);

    /**
     * Replaces an in-progress claim with a terminal failure record. Repeated
     * calls with the same key can then be rejected deterministically instead of
     * racing into the operation again.
     */
    void failProcessing(
            String tenantId,
            String idempotencyKey,
            String operationType,
            String errorCode,
            String message,
            Duration ttl);

    /**
     * Check whether a result has been recorded for the given key.
     * Note: advisory — the record may expire or be written immediately after.
     */
    boolean isProcessed(String tenantId, String idempotencyKey);

    enum BeginProcessingStatus {
        STARTED,
        REPLAY,
        CONFLICT,
        IN_PROGRESS,
        FAILED
    }

    record BeginProcessingResult(
            BeginProcessingStatus status,
            Optional<IdempotencyResult> result,
            Optional<IdempotencyFailure> failure
    ) {
        public BeginProcessingResult {
            if (status == null) {
                throw new IllegalArgumentException("status cannot be null");
            }
            result = result == null ? Optional.empty() : result;
            failure = failure == null ? Optional.empty() : failure;
        }

        public static BeginProcessingResult started() {
            return new BeginProcessingResult(BeginProcessingStatus.STARTED, Optional.empty(), Optional.empty());
        }

        public static BeginProcessingResult replay(IdempotencyResult result) {
            return new BeginProcessingResult(BeginProcessingStatus.REPLAY, Optional.of(result), Optional.empty());
        }

        public static BeginProcessingResult conflict() {
            return new BeginProcessingResult(BeginProcessingStatus.CONFLICT, Optional.empty(), Optional.empty());
        }

        public static BeginProcessingResult inProgress() {
            return new BeginProcessingResult(BeginProcessingStatus.IN_PROGRESS, Optional.empty(), Optional.empty());
        }

        public static BeginProcessingResult failed(IdempotencyFailure failure) {
            return new BeginProcessingResult(BeginProcessingStatus.FAILED, Optional.empty(), Optional.of(failure));
        }
    }

    /**
     * Input record used when persisting an idempotency result.
     * All non-null fields are trimmed to non-blank; {@code resultJson} and
     * {@code operationType} default to empty string.
     */
    record IdempotencyRecord(
            String tenantId,
            String idempotencyKey,
            String resultJson,
            Instant executedAt,
            String operationType
    ) {
        public IdempotencyRecord {
            if (tenantId == null || tenantId.isBlank()) {
                throw new IllegalArgumentException("tenantId cannot be blank");
            }
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new IllegalArgumentException("idempotencyKey cannot be blank");
            }
            if (executedAt == null) {
                executedAt = Instant.now();
            }
            if (operationType == null) {
                operationType = "";
            }
            if (resultJson == null) {
                resultJson = "";
            }
        }
    }

    /**
     * Output record returned by {@link #findResult(String, String)}.
     */
    record IdempotencyResult(
            String resultJson,
            Instant executedAt,
            String operationType
    ) {}

    record IdempotencyFailure(
            String errorCode,
            String message,
            Instant failedAt,
            String operationType
    ) {
        public IdempotencyFailure {
            if (failedAt == null) {
                failedAt = Instant.now();
            }
            if (errorCode == null) {
                errorCode = "";
            }
            if (message == null) {
                message = "";
            }
            if (operationType == null) {
                operationType = "";
            }
        }
    }
}
