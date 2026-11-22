package com.example.sis2.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "external.openlibrary")
public class ExternalCatalogProperties {

    /**
     * Base URL of the external book metadata service (OpenLibrary API).
     */
    @NotBlank
    private String baseUrl = "https://openlibrary.org";

    /**
     * Maximum time to wait while establishing a TCP connection to the external service.
     */
    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(2);

    /**
     * Maximum time to wait for data to be read from the external service after connection is established.
     */
    @NotNull
    private Duration readTimeout = Duration.ofSeconds(3);
}
