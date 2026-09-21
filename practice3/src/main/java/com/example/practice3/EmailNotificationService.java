package com.example.practice3;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service

@ConditionalOnProperty(name = "notification.type", havingValue = "email")
public class EmailNotificationService implements NotificationService {

    @Override
    public void send(String message) {
        System.out.println("--> send real email " + message);
    }
}