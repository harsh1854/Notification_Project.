
package com.harsh.notification.entity;

import com.harsh.notification.model.NotificationChannel;
import jakarta.persistence.*;

@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"recipient", "channel"}
                )
        }
)
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private boolean enabled;

    protected NotificationPreference() {
    }

    public NotificationPreference(
            String recipient,
            NotificationChannel channel,
            boolean enabled
    ) {
        this.recipient = recipient;
        this.channel = channel;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
