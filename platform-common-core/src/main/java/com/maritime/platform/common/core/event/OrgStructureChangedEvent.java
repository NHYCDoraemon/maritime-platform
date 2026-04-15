package com.maritime.platform.common.core.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 组织结构变更事件。
 *
 * <p>changeType 枚举值：
 * <ul>
 *   <li>{@code CREATE} — 新建组织（无需清缓存）</li>
 *   <li>{@code UPDATE} — 组织信息修改（名称等，无需清缓存）</li>
 *   <li>{@code DELETE} — 组织删除（清该 org 下所有用户缓存）</li>
 *   <li>{@code MOVE} — 组织层级调整/parent 变更（清子树用户 page 缓存，DEPT_TREE 受影响）</li>
 *   <li>{@code MERGE} — 组织合并（发告警通知管理员）</li>
 *   <li>{@code SPLIT} — 组织拆分（发告警通知管理员）</li>
 * </ul>
 *
 * <p>{@code affectedUserIds} 由事件发布方计算好，包含受影响组织及子树下的用户。</p>
 */
public record OrgStructureChangedEvent(
        String eventId,
        String orgCode,
        String changeType,
        String parentOrgCode,
        List<String> affectedUserIds,
        LocalDateTime occurredAt
) {

    public OrgStructureChangedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
