package com.maritime.platform.common.core.constant;

import java.util.Objects;

/**
 * 类型安全缓存 Key 构建器 — 所有 Key 通过静态方法生成，杜绝手拼字符串错误。
 *
 * <p>所有方法为纯函数（无副作用），参数为 null 时快速失败。</p>
 */
public final class IamCacheKeys {

    /** Placeholder for users without an active org (e.g. BUILTIN super admin) */
    private static final String NO_ORG = "_NO_ORG_";

    private IamCacheKeys() {
    }

    private static String orgOrDefault(String activeOrgCode) {
        return activeOrgCode != null && !activeOrgCode.isBlank()
                ? activeOrgCode : NO_ORG;
    }

    // ---- 权限快照 (TTL 30min, ±20% 随机化) ----

    /** iam:nav:{systemCode}:{userId}:{activeOrgCode} */
    public static String navSnapshot(String systemCode, String userId,
                                     String activeOrgCode) {
        Objects.requireNonNull(systemCode, "systemCode required");
        Objects.requireNonNull(userId, "userId required");
        // activeOrgCode nullable for BUILTIN super admin
        return "iam:nav:" + systemCode + ":" + userId + ":" + orgOrDefault(activeOrgCode);
    }

    /** iam:page:{systemCode}:{userId}:{activeOrgCode}:{pageCode} */
    public static String pageSnapshot(String systemCode, String userId,
                                      String activeOrgCode, String pageCode) {
        Objects.requireNonNull(systemCode, "systemCode required");
        Objects.requireNonNull(userId, "userId required");
        // activeOrgCode nullable for BUILTIN super admin
        Objects.requireNonNull(pageCode, "pageCode required");
        return "iam:page:" + systemCode + ":" + userId + ":"
                + activeOrgCode + ":" + pageCode;
    }

    /** 批量删除用 pattern: iam:nav:{systemCode}:{userId}:* */
    public static String navSnapshotPattern(String systemCode, String userId) {
        Objects.requireNonNull(systemCode, "systemCode required");
        Objects.requireNonNull(userId, "userId required");
        return "iam:nav:" + systemCode + ":" + userId + ":*";
    }

    /** 批量删除用 pattern: iam:page:{systemCode}:{userId}:* */
    public static String pageSnapshotPattern(String systemCode, String userId) {
        Objects.requireNonNull(systemCode, "systemCode required");
        Objects.requireNonNull(userId, "userId required");
        return "iam:page:" + systemCode + ":" + userId + ":*";
    }

    // ---- 管理员缓存 ----

    /** iam:admin:scope:{systemCode}:{userId} */
    public static String adminScope(String systemCode, String userId) {
        Objects.requireNonNull(systemCode, "systemCode required");
        Objects.requireNonNull(userId, "userId required");
        return "iam:admin:scope:" + systemCode + ":" + userId;
    }

    /** iam:admin:grantable:{systemCode}:{userId} */
    public static String adminGrantable(String systemCode, String userId) {
        Objects.requireNonNull(systemCode, "systemCode required");
        Objects.requireNonNull(userId, "userId required");
        return "iam:admin:grantable:" + systemCode + ":" + userId;
    }

    // ---- 默认权限 ----

    /** iam:perm:default:{systemCode} */
    public static String defaultPermission(String systemCode) {
        Objects.requireNonNull(systemCode, "systemCode required");
        return "iam:perm:default:" + systemCode;
    }

    // ---- 会话管理 ----

    /** iam:session:{sessionId} */
    public static String session(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId required");
        return "iam:session:" + sessionId;
    }

    /** iam:user:sessions:{userId} — 用户所有 sessionId 的 SET */
    public static String userSessions(String userId) {
        Objects.requireNonNull(userId, "userId required");
        return "iam:user:sessions:" + userId;
    }

    // ---- Token 黑名单 ----

    /** iam:token:blacklist:{jti} */
    public static String tokenBlacklist(String jti) {
        Objects.requireNonNull(jti, "jti required");
        return "iam:token:blacklist:" + jti;
    }

    // ---- 测试用户 ----

    /** iam:test:users — SET */
    public static String testUsers() {
        return "iam:test:users";
    }

    // ---- 用户组织关系 ----

    /** iam:user:orgs:{userId} */
    public static String userOrgs(String userId) {
        Objects.requireNonNull(userId, "userId required");
        return "iam:user:orgs:" + userId;
    }

    // ---- HMAC 签名 ----

    /** iam:app:auth:{systemCode} (HASH: appSecret/appCode/isEnabled) */
    public static String appAuth(String systemCode) {
        Objects.requireNonNull(systemCode, "systemCode required");
        return "iam:app:auth:" + systemCode;
    }

    /** iam:hmac:nonce:{nonce} — TTL 5min，防重放 */
    public static String hmacNonce(String nonce) {
        Objects.requireNonNull(nonce, "nonce required");
        return "iam:hmac:nonce:" + nonce;
    }

    // ---- 登录限流 ----

    /** iam:login:limit:{loginName} — 登录失败计数，TTL 60s */
    public static String loginLimit(String loginName) {
        Objects.requireNonNull(loginName, "loginName required");
        return "iam:login:limit:" + loginName;
    }

    /** iam:login:lock:{loginName} — 登录锁定标记，TTL 1800s */
    public static String loginLock(String loginName) {
        Objects.requireNonNull(loginName, "loginName required");
        return "iam:login:lock:" + loginName;
    }

    // ---- Refresh Token ----

    /** iam:refresh:{tokenId} — refresh token 映射 sessionId */
    public static String refreshToken(String tokenId) {
        Objects.requireNonNull(tokenId, "tokenId required");
        return "iam:refresh:" + tokenId;
    }

    // ---- 同步状态 ----

    /** iam:sync:status — 同步状态 HASH */
    public static String syncStatus() {
        return "iam:sync:status";
    }

    /** iam:sync:lock — 全量同步分布式锁 */
    public static String syncLock() {
        return "iam:sync:lock";
    }

    // ---- 事件去重 / 补偿 ----

    /** iam:event:dedup:{eventId} — 事件去重 SETNX，TTL 24h */
    public static String eventDedup(String eventId) {
        Objects.requireNonNull(eventId, "eventId required");
        return "iam:event:dedup:" + eventId;
    }

    /** iam:event:pending:{eventId} — 事件发布 pending 标记，TTL 60s */
    public static String eventPending(String eventId) {
        Objects.requireNonNull(eventId, "eventId required");
        return "iam:event:pending:" + eventId;
    }

    /** iam:event:history:{systemCode} — 事件回查 LIST，TTL 24h */
    public static String eventHistory(String systemCode) {
        Objects.requireNonNull(systemCode, "systemCode required");
        return "iam:event:history:" + systemCode;
    }

    /** iam:event:compensate:lock — 补偿扫描分布式锁，TTL 60s */
    public static String eventCompensateLock() {
        return "iam:event:compensate:lock";
    }

    /** iam:event:pending:* — SCAN pattern for pending events */
    public static String eventPendingPattern() {
        return "iam:event:pending:*";
    }

    /** 批量删除用 pattern: iam:nav:{systemCode}:*:* (全系统导航快照) */
    public static String navSystemPattern(String systemCode) {
        Objects.requireNonNull(systemCode, "systemCode required");
        return "iam:nav:" + systemCode + ":*";
    }

    /** 批量删除用 pattern: iam:page:{systemCode}:*:*:* (全系统页面快照) */
    public static String pageSystemPattern(String systemCode) {
        Objects.requireNonNull(systemCode, "systemCode required");
        return "iam:page:" + systemCode + ":*";
    }

    // ---- 分布式锁 ----

    /** iam:lock:snapshot:{systemCode}:{userId}:{activeOrgCode} — 快照重建互斥锁 */
    public static String snapshotLock(String systemCode, String userId,
                                      String activeOrgCode) {
        Objects.requireNonNull(systemCode, "systemCode required");
        Objects.requireNonNull(userId, "userId required");
        // activeOrgCode nullable for BUILTIN super admin
        return "iam:lock:snapshot:" + systemCode + ":" + userId
                + ":" + orgOrDefault(activeOrgCode);
    }

    /** iam:lock:snapshot:{systemCode}:{userId}:{activeOrgCode}:{pageCode} — page 快照重建互斥锁 */
    public static String snapshotLock(String systemCode, String userId,
                                      String activeOrgCode, String pageCode) {
        Objects.requireNonNull(systemCode, "systemCode required");
        Objects.requireNonNull(userId, "userId required");
        // activeOrgCode nullable for BUILTIN super admin
        Objects.requireNonNull(pageCode, "pageCode required");
        return "iam:lock:snapshot:" + systemCode + ":" + userId
                + ":" + orgOrDefault(activeOrgCode) + ":" + pageCode;
    }

    /** iam:perm:ver:{systemCode} — 权限版本原子计数器（Gateway 读取比对） */
    public static String permVersion(String systemCode) {
        Objects.requireNonNull(systemCode, "systemCode required");
        return "iam:perm:ver:" + systemCode;
    }

    /**
     * iam:user:enabled:{userId} — 用户启用状态标志（网关实时校验用）。
     * 值: "1"=启用, 不存在或"0"=禁用/已离职。
     * P0-B1 fix: 网关 JWT 验证时检查此标志，被禁用用户立即 403。
     */
    public static String userEnabled(String userId) {
        Objects.requireNonNull(userId, "userId required");
        return "iam:user:enabled:" + userId;
    }
}
