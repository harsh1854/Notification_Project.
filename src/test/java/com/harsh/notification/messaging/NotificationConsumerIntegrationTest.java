
package com.harsh.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harsh.notification.entity.NotificationStatus;
import com.harsh.notification.entity.ProcessedNotification;
import com.harsh.notification.repository.ProcessedNotificationRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.harsh.notification.dispatcher.NotificationDispatcher;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.harsh.notification.dispatcher.NotificationDispatcher;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class NotificationConsumerIntegrationTest {

    @MockitoSpyBean
    private NotificationDispatcher notificationDispatcher;


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

        // Publish the same event again
        kafkaTemplate.send("notification-events", id, eventJson)
                .get(10, TimeUnit.SECONDS);

        // Wait until the consumer has had time to handle the duplicate
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        verify(notificationDispatcher, times(1))
                                .dispatch(event)
                );
    }

}
