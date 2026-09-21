package com.example.practice_2;

import com.example.practice_2.config.MailProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MailPropertiesTest {

    @Autowired
    private MailProperties mailProperties;

    @Test
    void shouldLoadTestProfileProperties() {
        assertThat(mailProperties.host()).isEqualTo("test-mail.internal");
        assertThat(mailProperties.port()).isEqualTo(2525);
        assertThat(mailProperties.username()).isEqualTo("test-runner");
        assertThat(mailProperties.authEnabled()).isFalse();
        assertThat(mailProperties.timeout()).isEqualTo(Duration.ofSeconds(1));
    }
}