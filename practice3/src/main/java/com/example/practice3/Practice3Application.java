package com.example.practice3;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Practice3Application {

    public static void main(String[] args) {
        SpringApplication.run(Practice3Application.class, args);
    }

    @Bean
    public CommandLineRunner runner(OrderService orderService) {
        return args -> {
            orderService.createOrder("Ноутбук");
        };
    }

}
