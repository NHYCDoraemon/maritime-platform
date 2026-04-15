package com.maritime.platform.common.core.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 业务系统启停事件。
 *
 * <p>newStatus: {@code ENABLED} / {@code DISABLED}</p>
 */
public record AppEnabledEvent(
        String eventId,
        String systemCode,
        String newStatus,
        LocalDateTime occurredAt
) {

    public AppEnabledEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
