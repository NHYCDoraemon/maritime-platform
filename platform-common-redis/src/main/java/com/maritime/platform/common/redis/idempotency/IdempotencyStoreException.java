package com.maritime.platform.common.redis.idempotency;

/**
 * Raised when the idempotency backend cannot complete a critical state
 * transition. Callers should reject the guarded operation rather than execute
 * without an idempotency guard.
 */
public class IdempotencyStoreException extends RuntimeException {

    public IdempotencyStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
