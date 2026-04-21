package com.maritime.platform.common.tenant.context;

/**
 * Tenant context holder — reads/writes the current tenant ID via ThreadLocal.
 *
 * <p>Infrastructure layer (HTTP filter / Feign interceptor / MQ listener) is
 * responsible for calling {@link #set(String)} on request entry and
 * {@link #clear()} on exit. Application/domain layers read via
 * {@link #current()}.</p>
 *
 * <p>Domain code MUST NOT pass tenant ID as method parameter — always read
 * from {@code TenantContext.current()}.</p>
 */
public final class TenantContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private TenantContext() {}

    public static String current() {
        return HOLDER.get();
    }

    public static void set(String tenantId) {
        HOLDER.set(tenantId);
    }

    public static void clear() {
        HOLDER.remove();
    }
}