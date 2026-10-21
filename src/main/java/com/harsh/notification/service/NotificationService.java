package com.harsh.notification.service;

import com.harsh.notification.dto.NotificationRequest;
import com.harsh.notification.dto.NotificationResponse;
import com.harsh.notification.messaging.NotificationEvent;
import com.harsh.notification.messaging.NotificationProducer;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public final NotificationProducer notificationProducer;
    public NotificationService(NotificationProducer notificationProducer) {
        this.notificationProducer = notificationProducer;
    }

    public NotificationResponse sendNotification(
            NotificationRequest request) {

        // 1. Create the event
        NotificationEvent event = new NotificationEvent(
                request.getRecipient(),
                request.getMessage(),
                request.getChannel().name()
        );

        // 2. Send the event to Kafka
        notificationProducer.sendNotification(event);

        // 3. Return the API response
        return new NotificationResponse(
                true,
                "Notification accepted for processing",
                request.getRecipient(),
                request.getChannel().name()
        );
    }
}
