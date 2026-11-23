package com.example.sis3.messaging;

import com.example.sis3.dto.request.CreateBookRequest;
import com.example.sis3.dto.response.BookResponse;
import com.example.sis3.event.BookEvent;
import com.example.sis3.event.BookEventType;
import com.example.sis3.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.listener.auto-startup=true"
})
@EmbeddedKafka(partitions = 1, topics = {"book-events", "book-events.DLT"})
@ActiveProfiles("test")
@DirtiesContext
@DisplayName("SIS 3: Kafka Producer, Consumer & Dead-Letter Topic Integration Tests")
class BookEventKafkaIntegrationTest {

    @Autowired
    private BookEventProducer bookEventProducer;

    @Autowired
    private BookService bookService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoSpyBean
    private BookEventConsumer bookEventConsumer;

    @Test
    @DisplayName("Valid BookEvent published by producer should be consumed by BookEventConsumer")
    void sendValidEvent_ShouldBeConsumedByListener() {
        BookEvent event = BookEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(BookEventType.CREATED)
                .bookId(42L)
                .isbn("9780132350884")
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(new BigDecimal("39.99"))
                .occurredAt(Instant.now())
                .build();

        bookEventProducer.send(event);

        verify(bookEventConsumer, timeout(10000).times(1))
                .consumeBookEvent(any(String.class), eq("42"), anyInt(), anyLong());
    }

    @Test
    @DisplayName("Invalid payload should fail processing and be routed to Dead-Letter Topic")
    void sendInvalidPayload_ShouldBeRoutedToDlt() {
        String brokenPayload = "{\"invalid\": true}";

        kafkaTemplate.send("book-events", "999", brokenPayload);

        verify(bookEventConsumer, timeout(15000).times(1))
                .consumeDeadLetterEvent(eq(brokenPayload), any(), eq("book-events.DLT"), anyLong());
    }

    @Test
    @DisplayName("Creating and deleting a book via BookService should trigger @Audited, PostgreSQL commit, Kafka Producer & Consumer")
    void createAndDeleteBook_EndToEnd_ShouldPublishAfterCommitAndBeConsumed() {
        CreateBookRequest request = CreateBookRequest.builder()
                .title("Kafka End-to-End Test Book")
                .author("Test Author")
                .isbn("9789999999991")
                .price(new BigDecimal("29.99"))
                .publicationYear(2025)
                .description("End-to-end verification without Docker")
                .build();

        BookResponse created = bookService.create(request);
        String key = String.valueOf(created.getId());

        try {
            verify(bookEventConsumer, timeout(10000).times(1))
                    .consumeBookEvent(any(String.class), eq(key), anyInt(), anyLong());
        } finally {
            bookService.delete(created.getId());
        }

        verify(bookEventConsumer, timeout(10000).times(2))
                .consumeBookEvent(any(String.class), eq(key), anyInt(), anyLong());
    }
}
