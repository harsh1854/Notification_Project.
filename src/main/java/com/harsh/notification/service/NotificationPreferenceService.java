
package com.harsh.notification.service;

import com.harsh.notification.entity.NotificationPreference;
import com.harsh.notification.model.NotificationChannel;
import com.harsh.notification.repository.NotificationPreferenceRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;
    private final StringRedisTemplate redisTemplate;

    public NotificationPreferenceService(
            NotificationPreferenceRepository repository,
            StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public NotificationPreference updatePreference(
            String recipient,
            NotificationChannel channel,
            boolean enabled) {

        NotificationPreference preference =
                repository.findByRecipientAndChannel(recipient, channel)
                        .orElseGet(() ->
                                new NotificationPreference(
                                        recipient, channel, enabled));

        preference.setEnabled(enabled);

        NotificationPreference saved = repository.save(preference);

        String cacheKey = "notification-preference:"
                + recipient + ":" + channel;

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        redisTemplate.delete(cacheKey);
                    }
                }
        );

        return saved;
    }


    @Transactional(readOnly = true)
    public boolean getPreference(
            String recipient,
            NotificationChannel channel) {

        String cacheKey = "notification-preference:"
                + recipient + ":" + channel;

        String cachedValue = redisTemplate.opsForValue().get(cacheKey);

        if (cachedValue != null) {
            System.out.println("Preference cache HIT: " + cacheKey);
            return Boolean.parseBoolean(cachedValue);
        }

        System.out.println("Preference cache MISS: " + cacheKey);

        NotificationPreference preference =
                repository.findByRecipientAndChannel(recipient, channel)
                        .orElse(null);

        boolean enabled = preference != null && preference.isEnabled();

        redisTemplate.opsForValue().set(
                cacheKey,
                String.valueOf(enabled),
                java.time.Duration.ofMinutes(10)
        );

        return enabled;
    }

}
