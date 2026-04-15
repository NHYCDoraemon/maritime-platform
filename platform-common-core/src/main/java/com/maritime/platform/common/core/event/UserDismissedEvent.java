package com.maritime.platform.common.core.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户离职/停用事件。
 */
public record UserDismissedEvent(
        String eventId,
        String userId,
        String reason,
        LocalDateTime occurredAt
) {

    public UserDismissedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
