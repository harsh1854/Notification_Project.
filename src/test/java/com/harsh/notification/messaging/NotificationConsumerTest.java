
package com.harsh.notification.messaging;

import com.harsh.notification.dispatcher.NotificationDispatcher;
import com.harsh.notification.entity.ProcessedNotification;
import com.harsh.notification.repository.ProcessedNotificationRepository;
import com.harsh.notification.service.NotificationProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @Mock
    private NotificationProcessingService processingService;

    @Mock
    private ProcessedNotificationRepository repository;

    @InjectMocks
    private NotificationConsumer notificationConsumer;


    @Test
    void shouldProcessNotificationSuccessfully() {
        NotificationEvent event = new NotificationEvent(
                "test-id",
                "user@example.com",
                "Hello",
                "EMAIL"
        );

        when(processingService.claimNotification("test-id"))
                .thenReturn(true);

        doNothing().when(notificationDispatcher).dispatch(event);

        notificationConsumer.consumeNotification(event);

        verify(notificationDispatcher, times(1)).dispatch(event);
        verify(processingService, times(1)).markAsProcessed("test-id");
        verify(processingService, never()).markAsFailed("test-id");
    }

    @Test
    void shouldSkipAlreadyProcessedNotification() {
        NotificationEvent event = new NotificationEvent(
                "test-id",
                "user@example.com",
                "Hello",
                "EMAIL"
        );

        ProcessedNotification existing =
                new ProcessedNotification("test-id");
        existing.markAsProcessed();

        when(processingService.claimNotification("test-id"))
                .thenReturn(false);
        when(repository.findById("test-id"))
                .thenReturn(Optional.of(existing));

        notificationConsumer.consumeNotification(event);

        verify(repository, times(1)).findById("test-id");
        verify(notificationDispatcher, never()).dispatch(any());
        verify(processingService, never()).markAsProcessed("test-id");
        verify(processingService, never()).markAsFailed("test-id");
    }

    @Test
    void shouldMarkNotificationAsFailedWhenDispatchThrowsException() {
        NotificationEvent event = new NotificationEvent(
                "test-id",
                "user@example.com",
                "Hello",
                "EMAIL"
        );

        when(processingService.claimNotification("test-id"))
                .thenReturn(true);

        doThrow(new RuntimeException("Dispatch failed"))
                .when(notificationDispatcher).dispatch(event);

        assertThrows(
                RuntimeException.class,
                () -> notificationConsumer.consumeNotification(event)
        );

        verify(processingService, times(1)).markAsFailed("test-id");
        verify(processingService, never()).markAsProcessed("test-id");
    }

    @Test
    void shouldRetryFailedNotificationSuccessfully() {
        NotificationEvent event = new NotificationEvent(
                "test-id",
                "user@example.com",
                "Hello",
                "EMAIL"
        );

        ProcessedNotification existing =
                new ProcessedNotification("test-id");
        existing.markAsFailed();

        when(processingService.claimNotification("test-id"))
                .thenReturn(false);
        when(repository.findById("test-id"))
                .thenReturn(Optional.of(existing));
        when(processingService.retryFailedNotification("test-id"))
                .thenReturn(true);

        notificationConsumer.consumeNotification(event);

        verify(processingService, times(1))
                .retryFailedNotification("test-id");
        verify(notificationDispatcher, times(1)).dispatch(event);
        verify(processingService, times(1)).markAsProcessed("test-id");
        verify(processingService, never()).markAsFailed("test-id");
    }

    @Test
    void shouldThrowExceptionWhenRetryOfFailedNotificationFails() {
        NotificationEvent event = new NotificationEvent(
                "test-id",
                "user@example.com",
                "Hello",
                "EMAIL"
        );

        ProcessedNotification existing =
                new ProcessedNotification("test-id");
        existing.markAsFailed();

        when(processingService.claimNotification("test-id"))
                .thenReturn(false);
        when(repository.findById("test-id"))
                .thenReturn(Optional.of(existing));
        when(processingService.retryFailedNotification("test-id"))
                .thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                () -> notificationConsumer.consumeNotification(event)
        );

        verify(processingService, times(1))
                .retryFailedNotification("test-id");
        verify(notificationDispatcher, never()).dispatch(any());
    }

    @Test
    void shouldThrowExceptionWhenNotificationIsAlreadyProcessing() {
        NotificationEvent event = new NotificationEvent(
                "test-id",
                "user@example.com",
                "Hello",
                "EMAIL"
        );

        ProcessedNotification existing =
                new ProcessedNotification("test-id");

        when(processingService.claimNotification("test-id"))
                .thenReturn(false);
        when(repository.findById("test-id"))
                .thenReturn(Optional.of(existing));

        assertThrows(
                IllegalStateException.class,
                () -> notificationConsumer.consumeNotification(event)
        );

        verify(notificationDispatcher, never()).dispatch(any());
        verify(processingService, never()).markAsProcessed("test-id");
        verify(processingService, never()).markAsFailed("test-id");
    }

    @Test
    void shouldThrowExceptionWhenNotificationCannotBeClaimedAndDoesNotExist() {
        NotificationEvent event = new NotificationEvent(
                "missing-id",
                "user@example.com",
                "Hello",
                "EMAIL"
        );

        when(processingService.claimNotification("missing-id"))
                .thenReturn(false);
        when(repository.findById("missing-id"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> notificationConsumer.consumeNotification(event)
        );

        verify(notificationDispatcher, never()).dispatch(any());
    }

}
