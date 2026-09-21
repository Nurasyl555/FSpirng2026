package com.example.practice_2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan; //+

@SpringBootApplication
@ConfigurationPropertiesScan //+
public class Practice2Application {

    public static void main(String[] args) {
        SpringApplication.run(Practice2Application.class, args);
    }

}
