package com.harsh.notification.service;

import com.harsh.notification.dto.NotificationRequest;
import com.harsh.notification.dto.NotificationResponse;
import com.harsh.notification.entity.ProcessedNotification;
import com.harsh.notification.messaging.NotificationEvent;
import com.harsh.notification.messaging.NotificationProducer;
import com.harsh.notification.repository.ProcessedNotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationProducer notificationProducer;
    private final ProcessedNotificationRepository processedNotificationRepository;

    public NotificationService(
            NotificationProducer notificationProducer,
            ProcessedNotificationRepository processedNotificationRepository) {
        this.notificationProducer = notificationProducer;
        this.processedNotificationRepository = processedNotificationRepository;
    }

    public NotificationResponse sendNotification(
            NotificationRequest request) {

        // 1. Create the event with a unique ID
        NotificationEvent event = new NotificationEvent(
                java.util.UUID.randomUUID().toString(),
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

    public ProcessedNotification getNotificationStatus(String id) {
        return processedNotificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification not found: " + id
                ));
    }
}