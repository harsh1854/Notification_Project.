package com.harsh.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harsh.notification.dispatcher.NotificationDispatcher;
import com.harsh.notification.entity.NotificationStatus;
import com.harsh.notification.entity.ProcessedNotification;
import com.harsh.notification.repository.ProcessedNotificationRepository;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;

import com.harsh.notification.model.NotificationChannel;
import com.harsh.notification.service.NotificationPreferenceService;
import org.springframework.beans.factory.annotation.Autowired;


@SpringBootTest
class NotificationConsumerIntegrationTest {

    @Autowired
    private NotificationPreferenceService preferenceService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoSpyBean
    private NotificationDispatcher notificationDispatcher;

    @MockitoSpyBean
    private NotificationConsumer notificationConsumer;


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ProcessedNotificationRepository repository;

    @Test
    void shouldConsumeKafkaEventAndMarkNotificationAsProcessed() {
        // Arrange: create a unique notification event
        String id = UUID.randomUUID().toString();

        NotificationEvent event = new NotificationEvent(
                id,
                "integration-test@example.com",
                "Integration test notification",
                "EMAIL"
        );
        preferenceService.updatePreference(
                "integration-test@example.com",
                NotificationChannel.EMAIL,
                true
        );

        // Act: publish the event to the real Kafka topic
        try {
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String eventJson = objectMapper.writeValueAsString(event);

            kafkaTemplate.send("notification-events", id, eventJson)
                    .get(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish test event", e);
        }

        // Assert: wait until the real consumer updates PostgreSQL
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    var result = repository.findById(id);

                    assertTrue(
                            result.isPresent(),
                            "Notification should exist in PostgreSQL"
                    );

                    ProcessedNotification notification = result.get();

                    assertEquals(
                            NotificationStatus.PROCESSED,
                            notification.getStatus()
                    );
                });
    }


    @Test
    void shouldNotDispatchDuplicateProcessedNotification() throws Exception {
        String id = UUID.randomUUID().toString();

        NotificationEvent event = new NotificationEvent(
                id,
                "duplicate-test@example.com",
                "Duplicate test notification",
                "EMAIL"
        );

        preferenceService.updatePreference(
                "duplicate-test@example.com",
                NotificationChannel.EMAIL,
                true
        );

        ObjectMapper objectMapper = new ObjectMapper();
        String eventJson = objectMapper.writeValueAsString(event);

        // Clear previous invocations from other tests
        clearInvocations(notificationConsumer, notificationDispatcher);

        // Publish first event
        kafkaTemplate.send("notification-events", id, eventJson)
                .get(10, TimeUnit.SECONDS);

        // Wait for the first event to finish processing
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    var result = repository.findById(id);
                    assertTrue(result.isPresent());
                    assertEquals(
                            NotificationStatus.PROCESSED,
                            result.get().getStatus()
                    );
                });

        // Verify the first event was consumed and dispatched once
        verify(notificationConsumer, times(1))
                .consumeNotification(event);
        verify(notificationDispatcher, times(1))
                .dispatch(event);

        // Publish the same event again
        kafkaTemplate.send("notification-events", id, eventJson)
                .get(10, TimeUnit.SECONDS);

        // Wait until the consumer receives the duplicate event
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        verify(notificationConsumer, times(2))
                                .consumeNotification(event)
                );

        // Duplicate must not be dispatched again
        verify(notificationDispatcher, times(1))
                .dispatch(event);
    }


    @Test
    void shouldRetryFailedNotificationAndMarkItAsProcessed() throws Exception {
        String id = UUID.randomUUID().toString();

        // Arrange: PostgreSQL mein notification ko FAILED state mein create karo
        ProcessedNotification failedNotification = new ProcessedNotification(id);
        failedNotification.markAsFailed();
        repository.saveAndFlush(failedNotification);

        NotificationEvent event = new NotificationEvent(
                id,
                "retry-test@example.com",
                "Retry integration test",
                "EMAIL"
        );

        preferenceService.updatePreference(
                "retry-test@example.com",
                NotificationChannel.EMAIL,
                true
        );

        ObjectMapper objectMapper = new ObjectMapper();
        String eventJson = objectMapper.writeValueAsString(event);

        clearInvocations(notificationConsumer, notificationDispatcher);

        // Act: same ID ka Kafka event publish karo
        kafkaTemplate.send("notification-events", id, eventJson)
                .get(10, TimeUnit.SECONDS);

        // Assert: consumer FAILED record ko retry karke PROCESSED kare
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    ProcessedNotification notification =
                            repository.findById(id).orElseThrow();

                    assertEquals(
                            NotificationStatus.PROCESSED,
                            notification.getStatus()
                    );
                });

        verify(notificationDispatcher, timeout(1000).times(1))
                .dispatch(event);
    }


    @Test
    void shouldPublishFailedEventToDeadLetterTopic() throws Exception {
        String id = UUID.randomUUID().toString();

        NotificationEvent event = new NotificationEvent(
                id,
                "dlt-test@example.com",
                "DLT integration test",
                "EMAIL"
        );

        preferenceService.updatePreference(
                "dlt-test@example.com",
                NotificationChannel.EMAIL,
                true
        );

        ObjectMapper objectMapper = new ObjectMapper();
        String eventJson = objectMapper.writeValueAsString(event);

        clearInvocations(notificationConsumer, notificationDispatcher);

        doThrow(new RuntimeException("Intentional DLT test failure"))
                .when(notificationDispatcher)
                .dispatch(event);

        kafkaTemplate.send("notification-events", id, eventJson)
                .get(10, TimeUnit.SECONDS);

        // Wait until the consumer has exhausted retries and attempted recovery.
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() ->
                        verify(notificationDispatcher, times(3))
                                .dispatch(event)
                );

        // DLT message consumption/verification will be added next.
    }
}
