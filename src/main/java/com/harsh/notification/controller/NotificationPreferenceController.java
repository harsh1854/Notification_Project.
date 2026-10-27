
package com.harsh.notification.controller;

import com.harsh.notification.entity.NotificationPreference;
import com.harsh.notification.model.NotificationChannel;
import com.harsh.notification.service.NotificationPreferenceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    public NotificationPreferenceController(
            NotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @PutMapping
    public NotificationPreference updatePreference(
            @RequestParam String recipient,
            @RequestParam NotificationChannel channel,
            @RequestParam boolean enabled) {

        return preferenceService.updatePreference(
                recipient, channel, enabled);
    }

    @GetMapping
    public boolean getPreference(
            @RequestParam String recipient,
            @RequestParam NotificationChannel channel) {
        return preferenceService.getPreference(recipient, channel);
    }
}
