package com.maritime.iam.sdk.context;

import com.maritime.iam.sdk.model.PageSnapshot;
import java.util.List;

/**
 * ThreadLocal holder for IAM context within a single request.
 *
 * <p>Set by {@code IamPermissionFilter}, read by
 * {@code DataPermissionInjector} and {@code LineRoleFilter}.
 * Must be cleared in filter's finally block to prevent
 * thread pool reuse leaking context.
 */
public final class IamContext {

    private static final ThreadLocal<ContextData> HOLDER =
            new ThreadLocal<>();

    private IamContext() {
    }

    public record ContextData(
            String userId,
            String systemCode,
            String activeOrgCode,
            String pageCode,
            PageSnapshot pageSnapshot,
            List<String> lineRoles
    ) {

        public ContextData {
            lineRoles = lineRoles != null
                    ? List.copyOf(lineRoles) : List.of();
        }
    }

    public static void set(ContextData data) {
        HOLDER.set(data);
    }

    public static ContextData get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static String userId() {
        ContextData ctx = HOLDER.get();
        return ctx != null ? ctx.userId() : null;
    }

    public static String activeOrgCode() {
        ContextData ctx = HOLDER.get();
        return ctx != null ? ctx.activeOrgCode() : null;
    }

    public static List<String> lineRoles() {
        ContextData ctx = HOLDER.get();
        return ctx != null ? ctx.lineRoles() : List.of();
    }
}
