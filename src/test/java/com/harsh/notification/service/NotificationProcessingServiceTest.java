
package com.harsh.notification.service;

import com.harsh.notification.repository.ProcessedNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.harsh.notification.entity.NotificationStatus;
import com.harsh.notification.entity.ProcessedNotification;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class NotificationProcessingServiceTest {



    @Mock
    private ProcessedNotificationRepository repository;

    @InjectMocks
    private NotificationProcessingService processingService;


    @Test
    void shouldClaimNotificationSuccessfully() {
        when(repository.claimNotification(
                org.mockito.ArgumentMatchers.eq("test-id"),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class)
        )).thenReturn(1);

        boolean result = processingService.claimNotification("test-id");

        assertTrue(result);
        verify(repository, times(1))
                .claimNotification(
                        org.mockito.ArgumentMatchers.eq("test-id"),
                        org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class)
                );
    }


    @Test
    void shouldReturnFalseWhenNotificationCannotBeClaimed() {
        when(repository.claimNotification(
                org.mockito.ArgumentMatchers.eq("test-id"),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class)
        )).thenReturn(0);

        boolean result = processingService.claimNotification("test-id");

        assertFalse(result);
    }


    @Test
    void shouldRetryFailedNotificationSuccessfully() {
        when(repository.retryFailedNotification(
                org.mockito.ArgumentMatchers.eq("test-id"),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class)
        )).thenReturn(1);

        boolean result = processingService.retryFailedNotification("test-id");

        assertTrue(result);

        verify(repository, times(1))
                .retryFailedNotification(
                        org.mockito.ArgumentMatchers.eq("test-id"),
                        org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class)
                );
    }


    @Test
    void shouldReturnFalseWhenRetryFails() {
        when(repository.retryFailedNotification(
                org.mockito.ArgumentMatchers.eq("test-id"),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class)
        )).thenReturn(0);

        boolean result = processingService.retryFailedNotification("test-id");

        assertFalse(result);

        verify(repository, times(1))
                .retryFailedNotification(
                        org.mockito.ArgumentMatchers.eq("test-id"),
                        org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class)
                );
    }


    @Test
    void shouldMarkNotificationAsProcessed() {
        ProcessedNotification notification =
                new ProcessedNotification("test-id");

        when(repository.findById("test-id"))
                .thenReturn(Optional.of(notification));

        processingService.markAsProcessed("test-id");

        assertEquals(
                NotificationStatus.PROCESSED,
                notification.getStatus()
        );

        verify(repository, times(1)).findById("test-id");
    }

    @Test
    void shouldMarkNotificationAsFailed() {
        ProcessedNotification notification =
                new ProcessedNotification("test-id");

        when(repository.findById("test-id"))
                .thenReturn(Optional.of(notification));

        processingService.markAsFailed("test-id");

        assertEquals(
                NotificationStatus.FAILED,
                notification.getStatus()
        );

        verify(repository, times(1)).findById("test-id");
    }


    @Test
    void shouldThrowExceptionWhenNotificationIsNotFound() {
        when(repository.findById("missing-id"))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> processingService.markAsProcessed("missing-id")
        );

        verify(repository, times(1)).findById("missing-id");
    }

    @Test
    void shouldThrowExceptionWhenNotificationToMarkAsFailedIsNotFound() {
        when(repository.findById("missing-id"))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> processingService.markAsFailed("missing-id")
        );

        verify(repository, times(1)).findById("missing-id");
    }

    @Test
    void shouldPropagateExceptionWhenClaimNotificationFails() {
        when(repository.claimNotification(
                org.mockito.ArgumentMatchers.eq("test-id"),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class)
        )).thenThrow(new IllegalStateException("Database error"));

        assertThrows(
                IllegalStateException.class,
                () -> processingService.claimNotification("test-id")
        );

        verify(repository, times(1))
                .claimNotification(
                        org.mockito.ArgumentMatchers.eq("test-id"),
                        org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class)
                );
    }


    @Test
    void shouldThrowExceptionWhenMarkingAsFailedAndRepositoryThrows() {
        when(repository.findById("test-id"))
                .thenThrow(new IllegalStateException("Database error"));

        assertThrows(
                IllegalStateException.class,
                () -> processingService.markAsFailed("test-id")
        );

        verify(repository, times(1)).findById("test-id");
    }


    @Test
    void shouldThrowExceptionWhenMarkingAsProcessedAndRepositoryThrows() {
        when(repository.findById("test-id"))
                .thenThrow(new IllegalStateException("Database error"));

        assertThrows(
                IllegalStateException.class,
                () -> processingService.markAsProcessed("test-id")
        );

        verify(repository, times(1)).findById("test-id");
    }

}
