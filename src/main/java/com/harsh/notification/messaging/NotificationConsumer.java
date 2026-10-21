
package com.harsh.notification.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.harsh.notification.dispatcher.NotificationDispatcher;

@Service
public class NotificationConsumer {
    private final NotificationDispatcher notificationDispatcher;

    public NotificationConsumer(NotificationDispatcher notificationDispatcher) {
         this.notificationDispatcher = notificationDispatcher;
    }

    @KafkaListener(
            topics = "notification-events",
            groupId = "notification-group"
    )
    public void consumeNotification(NotificationEvent event) {
        notificationDispatcher.dispatch(event);
    }
}
