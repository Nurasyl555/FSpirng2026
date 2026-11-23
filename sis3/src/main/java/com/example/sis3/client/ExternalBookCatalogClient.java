package com.example.sis3.client;

import com.example.sis3.dto.response.ExternalBookMetadataResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalBookCatalogClient {

    public static final String SOURCE_OPEN_LIBRARY = "OPEN_LIBRARY_API";
    public static final String SOURCE_FALLBACK = "FALLBACK_LOCAL";

    private static final ParameterizedTypeReference<Map<String, OpenLibraryBookData>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient openLibraryRestClient;

    public ExternalBookMetadataResponse fetchMetadataByIsbn(String isbn, String fallbackTitle) {
        String bibKey = "ISBN:" + isbn;
        int maxAttempts = 2;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Calling external OpenLibrary API for {} (attempt {}/{})", bibKey, attempt, maxAttempts);
                return executeOpenLibraryCall(isbn, bibKey, fallbackTitle);
            } catch (Exception ex) {
                lastException = ex;
                log.warn("Attempt {}/{} failed for {} (reason: {})",
                        attempt, maxAttempts, bibKey, ex.getMessage());
            }
        }

        String reason = lastException != null ? lastException.getClass().getSimpleName() : "Unknown";
        log.warn("All {} attempts failed for {}. Returning fallback!", maxAttempts, bibKey);
        return buildFallback(isbn, fallbackTitle,
                "External service unavailable or timed out after " + maxAttempts + " attempts (" + reason + "); fallback activated");
    }

    private ExternalBookMetadataResponse executeOpenLibraryCall(String isbn, String bibKey, String fallbackTitle) {
        Map<String, OpenLibraryBookData> response = openLibraryRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/books")
                        .queryParam("bibkeys", bibKey)
                        .queryParam("format", "json")
                        .queryParam("jscmd", "data")
                        .build())
                .retrieve()
                .body(RESPONSE_TYPE);

        if (response == null || !response.containsKey(bibKey) || response.get(bibKey) == null) {
            log.warn("External OpenLibrary API returned no metadata for {}. Using fallback.", bibKey);
            return buildFallback(isbn, fallbackTitle,
                    "No entry found in external OpenLibrary catalog; returning local fallback metadata");
        }

        OpenLibraryBookData data = response.get(bibKey);
        String resolvedTitle = (data.title() != null && !data.title().isBlank())
                ? data.title()
                : (fallbackTitle != null ? fallbackTitle : "Unknown Title");
        String publisher = (data.publishers() != null && !data.publishers().isEmpty()
                && data.publishers().get(0).name() != null)
                ? data.publishers().get(0).name()
                : "Unknown Publisher";
        String previewUrl = (data.url() != null && !data.url().isBlank())
                ? data.url()
                : "https://openlibrary.org/isbn/" + isbn;

        return ExternalBookMetadataResponse.builder()
                .isbn(isbn)
                .title(resolvedTitle)
                .publisher(publisher)
                .numberOfPages(data.numberOfPages())
                .previewUrl(previewUrl)
                .source(SOURCE_OPEN_LIBRARY)
                .fallback(false)
                .message("Successfully fetched metadata from external OpenLibrary API")
                .retrievedAt(Instant.now())
                .build();
    }

    private ExternalBookMetadataResponse buildFallback(String isbn, String fallbackTitle, String reason) {
        return ExternalBookMetadataResponse.builder()
                .isbn(isbn)
                .title(fallbackTitle != null ? fallbackTitle : "Book (ISBN " + isbn + ")")
                .publisher("KBTU Bookstore Fallback Catalog")
                .numberOfPages(0)
                .previewUrl("https://openlibrary.org/isbn/" + isbn)
                .source(SOURCE_FALLBACK)
                .fallback(true)
                .message(reason)
                .retrievedAt(Instant.now())
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenLibraryBookData(
            String title,
            @JsonProperty("number_of_pages") Integer numberOfPages,
            String url,
            List<PublisherEntry> publishers
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PublisherEntry(String name) {}
}
