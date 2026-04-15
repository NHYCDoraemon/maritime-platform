package com.maritime.platform.common.core.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 岗位角色映射变更事件。
 *
 * <p>changeType: {@code ADD} / {@code REMOVE}</p>
 */
public record PositionRoleChangedEvent(
        String eventId,
        String systemCode,
        String positionCode,
        String roleCode,
        String changeType,
        List<String> affectedUserIds,
        LocalDateTime occurredAt
) {

    public PositionRoleChangedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
