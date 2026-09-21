package com.example.practice3;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final NotificationService notificationService;

    public OrderService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void createOrder(String itemName) {
        System.out.println("Заказ на товар '" + itemName + "' успешно создан.");

        notificationService.send("Создан новый заказ: " + itemName);
    }
}