package com.example.sis3.messaging;

import com.example.sis3.event.BookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.book-events}")
    private String bookEventsTopic;

    public void send(BookEvent event) {
        String key = String.valueOf(event.bookId());
        String payload = objectMapper.writeValueAsString(event);

        kafkaTemplate.send(bookEventsTopic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish BookEvent eventId={} bookId={} to topic={}: {}",
                                event.eventId(), event.bookId(), bookEventsTopic, ex.getMessage());
                    } else {
                        log.info("Published BookEvent type={} bookId={} eventId={} to topic={} partition={} offset={}",
                                event.eventType(),
                                event.bookId(),
                                event.eventId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}