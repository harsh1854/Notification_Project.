
package com.harsh.notification.dispatcher;

import com.harsh.notification.messaging.NotificationEvent;
import com.harsh.notification.model.NotificationChannel;
import com.harsh.notification.sender.NotificationSender;
import com.harsh.notification.service.NotificationPreferenceService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationDispatcher {

    private final List<NotificationSender> senders;
    private final NotificationPreferenceService preferenceService;

    public NotificationDispatcher(
            List<NotificationSender> senders,
            NotificationPreferenceService preferenceService) {
        this.senders = senders;
        this.preferenceService = preferenceService;
    }

//    public void dispatch(NotificationEvent event) {
//        NotificationChannel channel =
//                NotificationChannel.valueOf(event.channel());
//
//        boolean enabled = preferenceService.getPreference(
//                event.recipient(),
//                channel
//        );
//
//        if (!enabled) {
//            System.out.println(
//                    "Notification skipped: preference disabled. ID: "
//                            + event.id()
//            );
//            return;
//        }
//
//        NotificationSender sender = senders.stream()
//                .filter(s -> s.getChannel().equals(event.channel()))
//                .findFirst()
//                .orElseThrow(() ->
//                        new IllegalArgumentException(
//                                "Unknown channel: " + event.channel()
//                        )
//                );
//
//        sender.sendNotification(
//                event.recipient(),
//                event.message()
//        );
//    }

    public boolean dispatch(NotificationEvent event) {
        NotificationChannel channel =
                NotificationChannel.valueOf(event.channel());

        boolean enabled =
                preferenceService.getPreference(event.recipient(), channel);

        if (!enabled) {
            System.out.println(
                    "Notification skipped: preference disabled. ID: " + event.id());
            return false;
        }

        NotificationSender sender = senders.stream()
                .filter(s -> s.getChannel().equals(event.channel()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown channel: " + event.channel()));

        sender.sendNotification(event.recipient(), event.message());

        return true;
    }

}
