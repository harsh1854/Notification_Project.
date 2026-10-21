
package com.harsh.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    protected ProcessedNotification() {
    }

    public ProcessedNotification(String id) {
        this.id = id;
        this.processedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
