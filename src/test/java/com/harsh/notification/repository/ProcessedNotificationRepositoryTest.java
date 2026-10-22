package com.harsh.notification.repository;

import com.harsh.notification.entity.NotificationStatus;
import com.harsh.notification.entity.ProcessedNotification;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class ProcessedNotificationRepositoryTest {

    @Autowired
    private ProcessedNotificationRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAndFindNotification() {
        ProcessedNotification notification =
                new ProcessedNotification("integration-test-id");

        repository.saveAndFlush(notification);

        ProcessedNotification savedNotification =
                repository.findById("integration-test-id")
                        .orElseThrow();

        assertEquals(
                "integration-test-id",
                savedNotification.getId()
        );
    }

    @Test
    void shouldClaimNotificationSuccessfully() {
        int result = repository.claimNotification(
                "claim-test-id",
                java.time.LocalDateTime.now()
        );

        assertEquals(1, result);

        ProcessedNotification savedNotification =
                repository.findById("claim-test-id")
                        .orElseThrow();

        assertEquals(
                NotificationStatus.PROCESSING,
                savedNotification.getStatus()
        );
    }
    @Test
    void shouldNotInsertDuplicateNotification() {
        int firstResult = repository.claimNotification(
                "duplicate-test-id",
                LocalDateTime.now()
        );

        int secondResult = repository.claimNotification(
                "duplicate-test-id",
                LocalDateTime.now()
        );

        assertEquals(1, firstResult);
        assertEquals(0, secondResult);

        assertEquals(
                1,
                repository.findAll().stream()
                        .filter(n -> n.getId().equals("duplicate-test-id"))
                        .count()
        );
    }
    @Test
    void shouldRetryFailedNotificationSuccessfully() {
        ProcessedNotification notification =
                new ProcessedNotification("retry-test-id");

        notification.markAsFailed();
        repository.saveAndFlush(notification);

        int result = repository.retryFailedNotification(
                "retry-test-id",
                LocalDateTime.now()
        );

        assertEquals(1, result);

        entityManager.clear();

        ProcessedNotification updated =
                repository.findById("retry-test-id")
                        .orElseThrow();

        assertEquals(
                NotificationStatus.PROCESSING,
                updated.getStatus()
        );
    }

    @Test
    void shouldReturnZeroWhenRetryNotificationDoesNotExist() {
        int result = repository.retryFailedNotification(
                "missing-retry-id",
                LocalDateTime.now()
        );

        assertEquals(0, result);

        assertTrue(
                repository.findById("missing-retry-id").isEmpty()
        );
    }

    @Test
    void shouldReturnZeroWhenNotificationIsNotFailed() {
        ProcessedNotification notification =
                new ProcessedNotification("processing-retry-id");

        repository.saveAndFlush(notification);

        int result = repository.retryFailedNotification(
                "processing-retry-id",
                LocalDateTime.now()
        );

        assertEquals(0, result);
    }

}