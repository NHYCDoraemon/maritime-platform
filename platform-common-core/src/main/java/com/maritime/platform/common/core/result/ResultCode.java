package com.maritime.platform.common.core.result;

import com.maritime.platform.common.core.exception.ErrorCode;

public enum ResultCode implements ErrorCode {

    // --- Standard HTTP ---
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    METHOD_NOT_ALLOWED(405, "method not allowed"),
    CONFLICT(409, "conflict"),
    TOO_MANY_REQUESTS(429, "too many requests"),
    INTERNAL_ERROR(500, "internal error"),
    SERVICE_UNAVAILABLE(503, "service unavailable"),

    // --- Common business (1001-1099) ---
    PARAM_INVALID(1001, "parameter invalid"),
    DATA_NOT_FOUND(1002, "data not found"),
    DATA_DUPLICATE(1003, "data duplicate"),
    IDEMPOTENT_REJECTED(1005, "idempotent rejected"),
    PAGINATION_OFFSET_EXCEEDED(1007, "pagination offset exceeded"),
    OPTIMISTIC_LOCK_CONFLICT(1011, "optimistic lock conflict"),

    // --- Input security (1050-1059) ---
    INPUT_XSS_DETECTED(1050, "input xss detected"),

    // --- Auth module (1100-1199) ---
    AUTH_TOKEN_EXPIRED(1100, "auth token expired"),
    AUTH_TOKEN_INVALID(1101, "auth token invalid"),
    AUTH_REFRESH_TOKEN_EXPIRED(1102, "auth refresh token expired"),
    AUTH_OAUTH_CALLBACK_FAILED(1103, "auth oauth callback failed"),
    AUTH_SESSION_NOT_FOUND(1104, "auth session not found"),
    AUTH_LOCAL_LOGIN_FAILED(1105, "auth local login failed"),
    AUTH_USER_NOT_SYNCED(1110, "身份同步中，请稍后重试"),
    AUTH_USER_DISABLED(1111, "账户已在权限系统中停用"),
    AUTH_CREDENTIALS_INVALID(1112, "用户名或密码错误"),
    AUTH_ACCOUNT_LOCKED(1113, "账户已锁定，请30分钟后重试"),
    AUTH_PASSWORD_CHANGE_REQUIRED(1114, "首次登录请修改密码"),
    AUTH_ORG_SWITCH_DENIED(1115, "目标组织不在可切换范围内"),
    AUTH_SYNC_USER_LOCAL_LOGIN(1116, "SYNC用户请使用4A登录"),
    AUTH_PASSWORD_MISMATCH(1117, "旧密码不正确"),
    AUTH_SYNC_USER_PASSWORD(1118, "SYNC用户密码由4A管理"),

    // --- Permission (1150-1159) ---
    NO_PERMISSION(1150, "no permission"),

    // --- Admin module (1200-1299) ---
    ADMIN_APP_NOT_FOUND(1200, "admin app not found"),
    ADMIN_APP_DUPLICATE(1201, "admin app duplicate"),
    ADMIN_RESOURCE_NOT_FOUND(1202, "admin resource not found"),
    ADMIN_ROLE_NOT_FOUND(1203, "admin role not found"),
    ADMIN_ROLE_DUPLICATE(1204, "admin role duplicate"),
    ADMIN_ROLE_SYSTEM_PROTECTED(1205, "system role cannot be deleted"),
    ADMIN_DELEGATION_EXCEEDED(1206, "admin delegation exceeded"),
    ADMIN_PERMISSION_CEILING_EXCEEDED(1207, "admin permission ceiling exceeded"),
    ADMIN_ROLE_SCOPE_NOT_CUSTOM(1208, "role org scope type is not CUSTOM"),
    ADMIN_RESOURCE_TYPE_NOT_ALLOWED(1209, "resource type not allowed for default permission"),
    ADMIN_APP_STATUS_INVALID(1210, "admin app status invalid"),
    ADMIN_POSITION_ROLE_DUPLICATE(1211, "position role mapping already exists"),
    ADMIN_DEFAULT_PERM_DUPLICATE(1212, "default permission already exists"),
    ADMIN_SUPER_COUNT_GUARD(1213, "cannot remove last super admin"),
    ADMIN_NOT_AUTHORIZED(1214, "admin not authorized"),
    ADMIN_SCOPE_EXCEEDED(1215, "admin scope exceeded"),
    ADMIN_SELF_LOCK_PREVENTED(1216, "admin self-lock prevented"),
    BATCH_TOO_LARGE(1217, "batch too large, max 200"),
    ADMIN_USER_SOURCE_INVALID(1218, "only SYNC user can be super admin"),
    ADMIN_BUILTIN_PROTECTED(1219, "builtin super admin cannot be revoked"),
    ADMIN_DELEGATE_DUPLICATE(1220, "admin delegation already exists"),
    ADMIN_DELEGATION_DEPTH_EXCEEDED(1221, "delegation depth limit exceeded"),
    ADMIN_DELEGATE_CYCLE_DETECTED(1222, "delegation would create a cycle"),

    // --- Query module (1300-1399) ---
    QUERY_CACHE_MISS(1300, "query cache miss"),
    QUERY_PERMISSION_DENIED(1301, "query permission denied"),

    // --- Sync module (1400-1499) ---
    SYNC_DATA_PLATFORM_ERROR(1400, "sync data platform error"),
    SYNC_USER_NOT_FOUND(1401, "sync user not found"),
    SYNC_ORG_NOT_FOUND(1402, "sync org not found"),
    SYNC_LOCK_CONFLICT(1403, "同步任务正在执行中，请稍后重试"),
    SYNC_TEST_CHANNEL_DISABLED(1404, "测试通道未启用"),
    SYNC_EXTERNAL_USER_NOT_FOUND(1405, "编外用户不存在"),
    SYNC_EXTERNAL_ORG_NOT_FOUND(1406, "编外组织不存在"),

    // --- Data permission (1501-1519) ---
    DATA_PERMISSION_RULE_INVALID(1501, "invalid rule expression"),
    DATA_PERMISSION_RULE_INJECTION(1502, "rule expression contains forbidden keyword"),
    DATA_PERMISSION_SUBJECT_NOT_FOUND(1503, "data permission subject not found"),
    DATA_PERMISSION_RESOURCE_NOT_FOUND(1504, "data permission resource not found"),
    DATA_PERMISSION_DUPLICATE(1505, "data permission rule already exists"),

    // --- Audit module (1300-1310) ---
    ADMIN_AUDIT_SCOPE_DENIED(1302, "admin scope denied for audit query"),
    AUDIT_EXPORT_TOO_LARGE(1303, "导出数据超过 10 万条，请缩小时间范围"),

    // --- Event module (1520-1599) ---
    EVENT_PUBLISH_FAILED(1520, "event publish failed");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
