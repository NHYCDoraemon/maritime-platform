package com.maritime.platform.common.tenant.exception;

/**
 * Thrown when a method annotated with {@code @RequireTenantContext} is
 * invoked but {@link com.maritime.platform.common.tenant.context.TenantContext#current()}
 * returns null or blank.
 */
public class TenantContextMissingException extends RuntimeException {

    public TenantContextMissingException() {
        super("TenantContext.current() is missing — infrastructure layer must bind tenantId before entering application method");
    }

    public TenantContextMissingException(String methodSignature) {
        super("TenantContext.current() is missing when entering @RequireTenantContext method: " + methodSignature);
    }
}