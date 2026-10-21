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



////for fail testing that if it fail so same id try or not
//////}
//package com.harsh.notification.sender;
//
//import org.springframework.stereotype.Service;
//
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Service
//public class EmailSender implements NotificationSender {
//
//    private final Set<String> failedOnce =
//            ConcurrentHashMap.newKeySet();
//
//    @Override
//    public void sendNotification(String recipient, String message) {
//        if (message.contains("[FAIL-ONCE]")
//                && failedOnce.add(message)) {
//            throw new RuntimeException("Simulated one-time email failure");
//        }
//
//        System.out.println(
//                "Sending EMAIL notification to " + recipient + ": " + message
//        );
//    }
//
//    @Override
//    public String getChannel() {
//        return "EMAIL";
//    }
//}

