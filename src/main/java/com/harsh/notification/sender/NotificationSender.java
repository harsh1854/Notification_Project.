package com.harsh.notification.sender;

public interface NotificationSender {
    void sendNotification(String recipient, String message);
    String getChannel();

}
