package com.example.__practice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Hellocontroller {
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, Spring Boot faaaaaaaaaaaah";
    }
}
