
package com.harsh.notification.messaging;

import com.harsh.notification.dispatcher.NotificationDispatcher;
import com.harsh.notification.entity.NotificationStatus;
import com.harsh.notification.entity.ProcessedNotification;
import com.harsh.notification.repository.ProcessedNotificationRepository;
import com.harsh.notification.service.NotificationProcessingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationConsumer {

    private final NotificationDispatcher notificationDispatcher;
    private final NotificationProcessingService processingService;
    private final ProcessedNotificationRepository repository;

    public NotificationConsumer(
            NotificationDispatcher notificationDispatcher,
            NotificationProcessingService processingService,
            ProcessedNotificationRepository repository) {
        this.notificationDispatcher = notificationDispatcher;
        this.processingService = processingService;
        this.repository = repository;
    }

    @KafkaListener(
            topics = "notification-events",
            groupId = "notification-group"
    )
    public void consumeNotification(NotificationEvent event) {

        boolean claimed = processingService.claimNotification(event.id());

        if (!claimed) {
            ProcessedNotification existing =
                    repository.findById(event.id()).orElse(null);

            if (existing != null
                    && existing.getStatus() == NotificationStatus.PROCESSED) {
                System.out.println(
                        "Duplicate notification skipped. ID: " + event.id()
                );
                return;
            }

            throw new IllegalStateException(
                    "Notification already claimed but not completed. ID: "
                            + event.id()
            );
        }

        try {
            notificationDispatcher.dispatch(event);
            processingService.markAsProcessed(event.id());

            System.out.println(
                    "Notification processed successfully. ID: " + event.id()
            );
        } catch (Exception exception) {
            processingService.markAsFailed(event.id());

            System.out.println(
                    "Notification processing failed. ID: " + event.id()
            );

            throw exception;
        }
    }
}
