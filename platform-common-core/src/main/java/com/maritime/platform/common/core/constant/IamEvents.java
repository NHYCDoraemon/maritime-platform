package com.maritime.platform.common.core.constant;

/**
 * 事件类型与 MQ 拓扑常量。
 *
 * <p>常量一旦发布不可删除或重命名，只能新增。</p>
 */
public final class IamEvents {

    private IamEvents() {
    }

    // ---- 事件类型标识 ----

    public static final String PERMISSION_CHANGED = "PERMISSION_CHANGED";
    public static final String POSITION_ROLE_CHANGED = "POSITION_ROLE_CHANGED";
    public static final String ORG_STRUCTURE_CHANGED = "ORG_STRUCTURE_CHANGED";
    public static final String APP_ENABLED = "APP_ENABLED";
    public static final String USER_DISMISSED = "USER_DISMISSED";
    public static final String AUDIT = "AUDIT";

    // ---- MQ Exchange ----

    public static final String EXCHANGE_PERMISSION = "iam.permission.exchange";
    public static final String EXCHANGE_AUDIT = "iam.audit.exchange";

    // ---- MQ Routing Key (权限变更事件) ----

    public static final String ROUTING_PERM_CHANGED = "permission.changed";
    public static final String ROUTING_POSITION_ROLE_CHANGED = "permission.position-role.changed";
    public static final String ROUTING_ORG_CHANGED = "permission.org.changed";
    public static final String ROUTING_APP_ENABLED = "permission.app.enabled";
    public static final String ROUTING_USER_DISMISSED = "permission.user.dismissed";

    // ---- MQ Routing Key (审计事件 → 数据中台) ----

    public static final String ROUTING_AUDIT_AUTH = "audit.auth.login";
    public static final String ROUTING_AUDIT_GRANT = "audit.permission.grant";
    public static final String ROUTING_AUDIT_CHANGE = "audit.permission.change";
    public static final String ROUTING_AUDIT_DELEGATE = "audit.admin.delegate";
    public static final String ROUTING_AUDIT_RESOURCE = "audit.resource.change";
    public static final String ROUTING_AUDIT_SYNC = "audit.data.sync";
}
