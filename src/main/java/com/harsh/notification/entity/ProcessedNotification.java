package com.harsh.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_notifications")
public class ProcessedNotification {

    @Id
    @Column(nullable = false, unique = true)
    private String id;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    protected ProcessedNotification() {
    }

    public ProcessedNotification(String id) {
        this.id = id;
        this.processedAt = LocalDateTime.now();
        this.status = NotificationStatus.PROCESSING;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void markAsProcessed() {
        this.status = NotificationStatus.PROCESSED;
    }

    public void markAsFailed() {
        this.status = NotificationStatus.FAILED;
    }

    public void markAsSkipped() {
        this.status = NotificationStatus.SKIPPED;
    }
}