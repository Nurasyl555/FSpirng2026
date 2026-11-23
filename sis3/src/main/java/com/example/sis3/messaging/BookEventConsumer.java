package com.example.sis3.messaging;

import com.example.sis3.event.BookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.book-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeBookEvent(
            String payload,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        BookEvent event = objectMapper.readValue(payload, BookEvent.class);
        if (event.bookId() == null || event.eventType() == null) {
            throw new IllegalArgumentException("Invalid BookEvent payload: missing bookId or eventType");
        }

        log.info("Consumed BookEvent type={} bookId={} isbn={} key={} partition={} offset={}",
                event.eventType(), event.bookId(), event.isbn(), key, partition, offset);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.book-events-dlt}",
            groupId = "${spring.kafka.consumer.group-id}-dlt"
    )
    public void consumeDeadLetterEvent(
            String payload,
            @Header(value = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String exceptionMessage,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.error("Received message in DLT topic={} offset={} reason='{}' payload={}",
                topic, offset, exceptionMessage, payload);
    }
}