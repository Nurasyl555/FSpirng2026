package com.example.sis3.client;

import com.example.sis3.config.ExternalCatalogProperties;
import com.example.sis3.config.RestClientConfig;
import com.example.sis3.dto.response.ExternalBookMetadataResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("Practice 6: ExternalBookCatalogClient Timeout & Fallback Tests")
class ExternalBookCatalogClientTest {

    @Test
    @DisplayName("fetchMetadataByIsbn() should parse OpenLibrary JSON response when external service succeeds")
    void fetchMetadataByIsbn_WhenSuccess_ShouldReturnLiveMetadata() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://openlibrary.org");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        ExternalBookCatalogClient client = new ExternalBookCatalogClient(builder.build());

        String jsonPayload = """
                {
                  "ISBN:9780132350884": {
                    "title": "Clean Code",
                    "number_of_pages": 464,
                    "url": "https://openlibrary.org/books/OL24218235M/Clean_Code",
                    "publishers": [
                      { "name": "Prentice Hall" }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo("https://openlibrary.org/api/books?bibkeys=ISBN:9780132350884&format=json&jscmd=data"))
                .andRespond(withSuccess(jsonPayload, MediaType.APPLICATION_JSON));

        ExternalBookMetadataResponse response = client.fetchMetadataByIsbn("9780132350884", "Local Title");

        assertThat(response.isFallback()).isFalse();
        assertThat(response.getSource()).isEqualTo(ExternalBookCatalogClient.SOURCE_OPEN_LIBRARY);
        assertThat(response.getIsbn()).isEqualTo("9780132350884");
        assertThat(response.getTitle()).isEqualTo("Clean Code");
        assertThat(response.getPublisher()).isEqualTo("Prentice Hall");
        assertThat(response.getNumberOfPages()).isEqualTo(464);
        assertThat(response.getPreviewUrl()).isEqualTo("https://openlibrary.org/books/OL24218235M/Clean_Code");
        mockServer.verify();
    }

    @Test
    @DisplayName("fetchMetadataByIsbn() should retry and return fallback response when external service keeps returning 500 error")
    void fetchMetadataByIsbn_WhenServerError_ShouldReturnFallback() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://openlibrary.org");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        ExternalBookCatalogClient client = new ExternalBookCatalogClient(builder.build());

        mockServer.expect(requestTo("https://openlibrary.org/api/books?bibkeys=ISBN:9780132350884&format=json&jscmd=data"))
                .andRespond(withServerError());
        mockServer.expect(requestTo("https://openlibrary.org/api/books?bibkeys=ISBN:9780132350884&format=json&jscmd=data"))
                .andRespond(withServerError());

        ExternalBookMetadataResponse response = client.fetchMetadataByIsbn("9780132350884", "Clean Code");

        assertThat(response.isFallback()).isTrue();
        assertThat(response.getSource()).isEqualTo(ExternalBookCatalogClient.SOURCE_FALLBACK);
        assertThat(response.getTitle()).isEqualTo("Clean Code");
        assertThat(response.getPublisher()).isEqualTo("KBTU Bookstore Fallback Catalog");
        assertThat(response.getMessage()).contains("fallback activated");
        mockServer.verify();
    }

    @Test
    @DisplayName("fetchMetadataByIsbn() should retry after transient 500 error and succeed on second attempt")
    void fetchMetadataByIsbn_WhenFirstAttemptFailsAndSecondSucceeds_ShouldReturnLiveMetadata() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://openlibrary.org");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        ExternalBookCatalogClient client = new ExternalBookCatalogClient(builder.build());

        String jsonPayload = """
                {
                  "ISBN:9780132350884": {
                    "title": "Clean Code",
                    "number_of_pages": 464,
                    "url": "https://openlibrary.org/books/OL24218235M/Clean_Code",
                    "publishers": [
                      { "name": "Prentice Hall" }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo("https://openlibrary.org/api/books?bibkeys=ISBN:9780132350884&format=json&jscmd=data"))
                .andRespond(withServerError());
        mockServer.expect(requestTo("https://openlibrary.org/api/books?bibkeys=ISBN:9780132350884&format=json&jscmd=data"))
                .andRespond(withSuccess(jsonPayload, MediaType.APPLICATION_JSON));

        ExternalBookMetadataResponse response = client.fetchMetadataByIsbn("9780132350884", "Local Title");

        assertThat(response.isFallback()).isFalse();
        assertThat(response.getSource()).isEqualTo(ExternalBookCatalogClient.SOURCE_OPEN_LIBRARY);
        assertThat(response.getTitle()).isEqualTo("Clean Code");
        assertThat(response.getPublisher()).isEqualTo("Prentice Hall");
        mockServer.verify();
    }

    @Test
    @DisplayName("fetchMetadataByIsbn() should return fallback when external service exceeds configured readTimeout")
    void fetchMetadataByIsbn_WhenReadTimeoutExceeded_ShouldReturnFallback() throws Exception {
        HttpServer slowServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slowServer.createContext("/api/books", exchange -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        slowServer.start();

        try {
            int port = slowServer.getAddress().getPort();
            ExternalCatalogProperties properties = new ExternalCatalogProperties();
            properties.setBaseUrl("http://127.0.0.1:" + port);
            properties.setConnectTimeout(Duration.ofMillis(200));
            properties.setReadTimeout(Duration.ofMillis(100));

            RestClient restClient = new RestClientConfig().openLibraryRestClient(properties);
            ExternalBookCatalogClient client = new ExternalBookCatalogClient(restClient);

            ExternalBookMetadataResponse response = client.fetchMetadataByIsbn("9780132350884", "Clean Code");

            assertThat(response.isFallback()).isTrue();
            assertThat(response.getSource()).isEqualTo(ExternalBookCatalogClient.SOURCE_FALLBACK);
            assertThat(response.getTitle()).isEqualTo("Clean Code");
            assertThat(response.getMessage()).contains("fallback activated");
        } finally {
            slowServer.stop(0);
        }
    }
}
