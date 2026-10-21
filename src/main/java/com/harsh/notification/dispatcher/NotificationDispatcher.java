package com.harsh.notification.dispatcher;

import com.harsh.notification.messaging.NotificationEvent;
import com.harsh.notification.sender.NotificationSender;
import org.springframework.stereotype.Service;

import java.util.List;



@Service
public class NotificationDispatcher {

    private final List<NotificationSender> senders;
    public NotificationDispatcher( List<NotificationSender> senders) {
        this.senders = senders;

        senders.forEach(sender ->
                System.out.println(
                        "Registered sender channel: " + sender.getChannel()
                ));
    }

    public void dispatch(NotificationEvent event) {

        senders.stream()
                .filter(sender -> sender.getChannel().equals(event.channel()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown channel: " + event.channel()
                ))
                .sendNotification(event.recipient(), event.message());
    }
}
