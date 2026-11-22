package com.example.sis2.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "KBTU Central Bookstore REST API",
                version = "1.0.0",
                description = "Practice 6 — Documented Bookstore REST API with Swagger UI (springdoc-openapi) "
                        + "and resilient external OpenLibrary service integration with configured timeouts and fallback.",
                contact = @Contact(
                        name = "KBTU Backend Framework Course (INFT3132)",
                        email = "bookstore@kbtu.kz"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development Server")
        }
)
public class OpenApiConfig {
}
