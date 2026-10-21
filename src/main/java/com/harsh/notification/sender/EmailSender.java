package com.harsh.notification.sender;

import org.springframework.stereotype.Service;

@Service
public class EmailSender implements NotificationSender {
    @Override
    public void sendNotification(String recipient, String message) {
        System.out.println("Sending EMAIL notification to " + recipient + ": " + message);
    }
    @Override
    public String getChannel() {
        return "EMAIL";
    }
}
