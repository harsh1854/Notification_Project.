package com.harsh.notification.sender;

import org.springframework.stereotype.Service;

@Service
public class SmsSender implements NotificationSender{
    @Override
    public void sendNotification(String recipient, String message) {
        System.out.println("Sending notification to " + recipient+": "+message);
    }
    @Override
    public String getChannel() {
        return "Sms";
    }
}
