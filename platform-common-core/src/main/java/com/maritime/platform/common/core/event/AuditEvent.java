package com.maritime.platform.common.core.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审计事件 — 发布到数据中台存档。
 *
 * <p>{@code batchId} 用于批量操作聚合查询，非批量操作时为 null。</p>
 */
public record AuditEvent(
        String eventId,
        String operatorUserId,
        String operationType,
        String systemCode,
        String targetType,
        String targetCode,
        String detailJson,
        String batchId,
        LocalDateTime occurredAt
) {

    public AuditEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
