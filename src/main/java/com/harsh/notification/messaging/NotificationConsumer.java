
package com.harsh.notification.messaging;

import com.harsh.notification.dispatcher.NotificationDispatcher;
import com.harsh.notification.entity.ProcessedNotification;
import com.harsh.notification.repository.ProcessedNotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationConsumer {

    private final NotificationDispatcher notificationDispatcher;
    private final ProcessedNotificationRepository repository;

    public NotificationConsumer(
            NotificationDispatcher notificationDispatcher,
            ProcessedNotificationRepository repository) {
        this.notificationDispatcher = notificationDispatcher;
        this.repository = repository;
    }

    @Transactional
    @KafkaListener(
            topics = "notification-events",
            groupId = "notification-group"
    )
    public void consumeNotification(NotificationEvent event) {

        // 1. Check whether this event was already processed
        if (repository.existsById(event.id())) {
            System.out.println(
                    "Duplicate notification skipped. ID: " + event.id()
            );
            return;
        }

        // 2. Process the notification
        notificationDispatcher.dispatch(event);

        // 3. Record successful processing
        repository.save(new ProcessedNotification(event.id()));

        System.out.println(
                "Notification processed and saved. ID: " + event.id()
        );
    }
}
