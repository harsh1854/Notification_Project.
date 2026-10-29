
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

            if (existing == null) {
                throw new IllegalStateException(
                        "Could not claim notification. ID: " + event.id());
            }

            if (existing.getStatus() == NotificationStatus.PROCESSED
                    || existing.getStatus() == NotificationStatus.SKIPPED) {

                System.out.println(
                        "Duplicate or already skipped notification ignored. ID: "
                                + event.id());

                return;
            }

            if (existing.getStatus() == NotificationStatus.FAILED) {
                boolean retried =
                        processingService.retryFailedNotification(event.id());

                if (!retried) {
                    throw new IllegalStateException(
                            "Could not retry failed notification. ID: "
                                    + event.id());
                }
            } else {
                throw new IllegalStateException(
                        "Notification is already being processed. ID: "
                                + event.id());
            }
        }

        try {
            boolean dispatched = notificationDispatcher.dispatch(event);

            if (dispatched) {
                processingService.markAsProcessed(event.id());

                System.out.println(
                        "Notification processed successfully. ID: " + event.id()
                );
            } else {
                processingService.markAsSkipped(event.id());

                System.out.println(
                        "Notification skipped due to preference. ID: " + event.id()
                );
            }

        }  catch (Exception exception) {
        try {
            processingService.markAsFailed(event.id());
        } catch (Exception statusException) {
            exception.addSuppressed(statusException);
        }

        System.err.println(
                "Notification processing failed. ID: " + event.id()
        );

        exception.printStackTrace(); // Original error print karega

        throw exception;
    }
    }
}
