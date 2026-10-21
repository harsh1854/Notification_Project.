package com.harsh.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationResponse {
    private boolean success;
    private String message;
    private String recipient;
    private String channel;
}
