package com.harsh.notification.controller;
import com.harsh.notification.dto.NotificationRequest;
import com.harsh.notification.dto.NotificationResponse;
import com.harsh.notification.entity.ProcessedNotification;
import com.harsh.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
        public NotificationResponse sendNotification(@Valid @RequestBody NotificationRequest request) {
            return notificationService.sendNotification(request);
        }

    @GetMapping("/{id}")
    public ProcessedNotification getNotificationStatus(
            @PathVariable String id) {
        return notificationService.getNotificationStatus(id);
    }
}
