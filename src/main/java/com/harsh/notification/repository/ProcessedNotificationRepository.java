
package com.harsh.notification.repository;

import com.harsh.notification.entity.ProcessedNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedNotificationRepository
        extends JpaRepository<ProcessedNotification, String> {
}
