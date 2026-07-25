package com.maritime.iam.sdk.permission;

/**
 * Signals that an older IAM deployment does not expose the additive
 * permission-version endpoint.
 */
final class PermissionVersionEndpointUnsupportedException
        extends RuntimeException {

    PermissionVersionEndpointUnsupportedException() {
        super("IAM permission-version endpoint is unavailable");
    }
}
