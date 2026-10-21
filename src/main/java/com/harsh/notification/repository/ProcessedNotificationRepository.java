
package com.harsh.notification.repository;

import com.harsh.notification.entity.ProcessedNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ProcessedNotificationRepository
        extends JpaRepository<ProcessedNotification, String> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_notifications (id, processed_at, status)
            VALUES (:id, :processedAt, 'PROCESSING')
            ON CONFLICT (id) DO NOTHING
            """, nativeQuery = true)
    int claimNotification(
            @Param("id") String id,
            @Param("processedAt") LocalDateTime processedAt
    );

    @Modifying
    @Query(value = """
            UPDATE processed_notifications
            SET status = 'PROCESSING',
                processed_at = :processedAt
            WHERE id = :id
              AND status = 'FAILED'
            """, nativeQuery = true)
    int retryFailedNotification(
            @Param("id") String id,
            @Param("processedAt") LocalDateTime processedAt
    );
}
