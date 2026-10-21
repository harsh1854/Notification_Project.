
package com.harsh.notification.service;

import com.harsh.notification.entity.NotificationStatus;
import com.harsh.notification.entity.ProcessedNotification;
import com.harsh.notification.repository.ProcessedNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationProcessingService {

    private final ProcessedNotificationRepository repository;

    public NotificationProcessingService(
            ProcessedNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean claimNotification(String id) {
        int claimed = repository.claimNotification(
                id,
                LocalDateTime.now()
        );

        return claimed == 1;
    }

    @Transactional
    public void markAsProcessed(String id) {
        ProcessedNotification notification =
                repository.findById(id).orElseThrow();

        notification.markAsProcessed();
    }

    @Transactional
    public void markAsFailed(String id) {
        ProcessedNotification notification =
                repository.findById(id).orElseThrow();

        notification.markAsFailed();
    }
}
