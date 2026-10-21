package com.harsh.notification.messaging;
import java.util.UUID;

public record NotificationEvent(
        String id,
        String recipient,
        String message,
        String channel
) {
    public NotificationEvent {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }
}
