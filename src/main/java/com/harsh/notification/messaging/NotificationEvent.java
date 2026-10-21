package com.harsh.notification.messaging;

public record  NotificationEvent (
        String recipient,
        String message,
        String channel
)  {
}
