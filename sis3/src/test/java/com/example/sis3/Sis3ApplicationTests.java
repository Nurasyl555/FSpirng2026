package com.example.sis3;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Sis3Application Context Load Test")
class Sis3ApplicationTests {

    @Test
    void contextLoads() {
    }
}
