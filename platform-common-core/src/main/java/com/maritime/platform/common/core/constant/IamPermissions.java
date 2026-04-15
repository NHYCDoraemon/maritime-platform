package com.maritime.platform.common.core.constant;

/**
 * 权限常量注册中心 — 集中定义所有权限码常量。
 *
 * <p>供 {@code @RequirePermission} 注解和前端判断使用。
 * 常量一旦发布不可删除或重命名，只能新增。</p>
 */
public final class IamPermissions {

    private IamPermissions() {
    }

    // ---- IAM 管理后台角色码 (role_domain=ADMIN, systemCode=IAM) ----

    public static final String IAM_SUPER_ADMIN = "IAM_SUPER_ADMIN";
    public static final String IAM_SYSTEM_ADMIN = "IAM_SYSTEM_ADMIN";
    public static final String IAM_OPS_ADMIN = "IAM_OPS_ADMIN";
    public static final String IAM_AUDIT_ADMIN = "IAM_AUDIT_ADMIN";

    // ---- IAM 管理后台菜单级权限码 (systemCode=IAM) ----

    public static final String CONSOLE_DASHBOARD = "iam:console:dashboard";
    public static final String CONSOLE_APP_MGMT = "iam:console:appMgmt";
    public static final String CONSOLE_ADMIN_MGMT = "iam:console:adminMgmt";
    public static final String CONSOLE_USER_ORG = "iam:console:userOrg";
    public static final String CONSOLE_RESOURCE = "iam:console:resource";
    public static final String CONSOLE_ROLE = "iam:console:role";
    public static final String CONSOLE_GRANT = "iam:console:grant";
    public static final String CONSOLE_DATA_PERM = "iam:console:dataPerm";
    public static final String CONSOLE_DEFAULT_PERM = "iam:console:defaultPerm";
    public static final String CONSOLE_REVERSE_QUERY = "iam:console:reverseQuery";
    public static final String CONSOLE_AUDIT = "iam:console:audit";
    public static final String CONSOLE_OPS = "iam:console:ops";

    // ---- 管理端功能权限码 (用于 @RequirePermission) ----

    public static final String ADMIN_APP_MANAGE = "admin:app:manage";
    public static final String ADMIN_ROLE_MANAGE = "admin:role:manage";
    public static final String ADMIN_RESOURCE_MANAGE = "admin:resource:manage";
    public static final String ADMIN_GRANT_MANAGE = "admin:grant:manage";
    public static final String ADMIN_DELEGATE = "admin:delegate";
    public static final String ADMIN_AUDIT_VIEW = "admin:audit:view";
    public static final String ADMIN_OPS_MANAGE = "admin:ops:manage";

    // ---- 同步管理权限码 ----

    public static final String ADMIN_EXTERNAL_USER_MANAGE = "admin:external-user:manage";
    public static final String ADMIN_EXTERNAL_ORG_MANAGE = "admin:external-org:manage";
    public static final String ADMIN_TEST_USER_MANAGE = "admin:test-user:manage";
    public static final String ADMIN_SYNC_TRIGGER = "admin:sync:trigger";
    public static final String ADMIN_SYNC_STATUS = "admin:sync:status";

    // ---- 操作类型常量 (用于 @OperationAudit + iam_audit_log.operation_type) ----

    public static final String OP_ROLE_CREATE = "ROLE_CREATE";
    public static final String OP_ROLE_UPDATE = "ROLE_UPDATE";
    public static final String OP_ROLE_DELETE = "ROLE_DELETE";
    public static final String OP_GRANT = "GRANT";
    public static final String OP_DENY = "DENY";
    public static final String OP_REVOKE = "REVOKE";
    public static final String OP_DELEGATE = "DELEGATE";
    public static final String OP_RESOURCE_CREATE = "RESOURCE_CREATE";
    public static final String OP_RESOURCE_UPDATE = "RESOURCE_UPDATE";
    public static final String OP_RESOURCE_DELETE = "RESOURCE_DELETE";
    public static final String OP_APP_REGISTER = "APP_REGISTER";
    public static final String OP_APP_ENABLE = "APP_ENABLE";
    public static final String OP_APP_DISABLE = "APP_DISABLE";
    public static final String OP_DEFAULT_PERM_UPDATE = "DEFAULT_PERM_UPDATE";
    public static final String OP_DATA_PERM_UPDATE = "DATA_PERM_UPDATE";
    public static final String OP_LINE_POLICY_UPDATE = "LINE_POLICY_UPDATE";
    public static final String OP_POSITION_ROLE_UPDATE = "POSITION_ROLE_UPDATE";
    public static final String OP_BATCH_GRANT = "BATCH_GRANT";
    public static final String OP_ORG_ROLE_GRANT = "ORG_ROLE_GRANT";
    public static final String OP_ORG_ROLE_DENY = "ORG_ROLE_DENY";
    public static final String OP_ORG_ROLE_REVOKE = "ORG_ROLE_REVOKE";
    public static final String OP_DELEGATE_CREATE = "DELEGATE_CREATE";
    public static final String OP_DELEGATE_REVOKE = "DELEGATE_REVOKE";
    public static final String OP_SUPER_ADMIN_DESIGNATE = "SUPER_ADMIN_DESIGNATE";
    public static final String OP_SUPER_ADMIN_REVOKE = "SUPER_ADMIN_REVOKE";
    public static final String ADMIN_SUPER_MANAGE = "admin:super:manage";

    // ---- 系统操作员标识 (自动注册等非人工操作的 operator) ----

    public static final String OPERATOR_SYSTEM = "SYSTEM";
}
