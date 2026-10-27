
package com.harsh.notification.repository;

import com.harsh.notification.entity.NotificationPreference;
import com.harsh.notification.model.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationPreferenceRepository
        extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByRecipientAndChannel(
            String recipient,
            NotificationChannel channel
    );
}
