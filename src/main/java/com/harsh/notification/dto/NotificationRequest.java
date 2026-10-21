package com.harsh.notification.dto;

import com.harsh.notification.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
public class NotificationRequest {
    @NotBlank(message = "Recipient cannot be blank")
    private String recipient;
    @NotBlank(message = "Message cannot be blank")
    private String message;

    private NotificationChannel channel;
}
