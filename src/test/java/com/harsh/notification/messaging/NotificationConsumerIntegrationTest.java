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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.timeout;

@SpringBootTest
class NotificationConsumerIntegrationTest {

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

}
