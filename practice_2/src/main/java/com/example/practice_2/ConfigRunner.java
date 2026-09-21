package com.example.practice_2;

import com.example.practice_2.config.MailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ConfigRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigRunner.class);

    private final MailProperties mailProperties;


    public ConfigRunner(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    @Override
    public void run(String... args) {
        log.info("==========================================");
        log.info("Загруженные настройки (Dev Profile):");
        log.info("Host: {}", mailProperties.host());
        log.info("Port: {}", mailProperties.port());
        log.info("Username: {}", mailProperties.username());
        log.info("Timeout: {}", mailProperties.timeout());
        log.info("Auth Enabled: {}", mailProperties.authEnabled());
        log.info("==========================================");
    }
}