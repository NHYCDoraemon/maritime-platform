package com.maritime.platform.common.core.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 权限变更事件 — 缓存失效信号。
 *
 * <p>changeType 枚举值：
 * <ul>
 *   <li>{@code ROLE_RESOURCE} — 角色资源绑定变更</li>
 *   <li>{@code ROLE_ATTR} — 角色属性变更 (enabled_flag/org_scope_type/role_domain)</li>
 *   <li>{@code USER_ROLE} — 用户角色 GRANT/DENY/REVOKE</li>
 *   <li>{@code ORG_ROLE} — 组织角色绑定变更</li>
 *   <li>{@code POSITION_ROLE} — 岗位角色映射变更</li>
 *   <li>{@code DEFAULT_PERM} — 默认权限变更（清全系统 nav 缓存）</li>
 *   <li>{@code DATA_PERM} — 数据权限规则变更</li>
 *   <li>{@code LINE_POLICY} — 五条线策略变更（清全系统 page 缓存）</li>
 * </ul>
 *
 * <p>{@code affectedUserIds} 由事件发布方（admin-service）在事务提交前计算好。
 * 为 null 时表示影响全系统用户（如 DEFAULT_PERM / LINE_POLICY），消费方降级为 SCAN 全量清理。</p>
 */
public record PermissionChangedEvent(
        String eventId,
        String systemCode,
        String roleCode,
        String changeType,
        long version,
        List<String> affectedUserIds,
        LocalDateTime occurredAt
) {

    public PermissionChangedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
