package com.example.practice3;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service

@ConditionalOnProperty(name = "notification.type", havingValue = "console", matchIfMissing = true)
public class ConsoleNotificationService implements NotificationService {

    @Override
    public void send(String message) {
        System.out.println("--> Лог в консоль (Console): " + message);
    }
}