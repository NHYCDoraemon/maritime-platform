package com.maritime.iam.sdk.permission;

/**
 * Raised when IAM permission state cannot be verified safely.
 *
 * <p>Consumers should fail closed and return a temporary-unavailable
 * response instead of continuing with stale grants.</p>
 */
public class IamPermissionUnavailableException
        extends RuntimeException {

    public IamPermissionUnavailableException(String message) {
        super(message);
    }

    public IamPermissionUnavailableException(
            String message, Throwable cause) {
        super(message, cause);
    }
}
