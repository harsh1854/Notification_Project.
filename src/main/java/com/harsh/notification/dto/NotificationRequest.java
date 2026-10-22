package com.harsh.notification.dto;

import com.harsh.notification.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import jakarta.validation.constraints.Email;

@Getter
@Setter
public class NotificationRequest {
    @Email(message = "Recipient must be a valid email address")
    @NotBlank(message = "Recipient cannot be blank")
    private String recipient;
    @NotBlank(message = "Message cannot be blank")
    private String message;
    @NotNull(message = "Channel cannot be null")
    private NotificationChannel channel;
}
