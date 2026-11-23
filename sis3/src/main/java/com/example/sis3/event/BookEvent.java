package com.example.sis3.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record BookEvent(
        UUID eventId,
        BookEventType eventType,
        Long bookId,
        String isbn,
        String title,
        String author,
        BigDecimal price,
        Instant occurredAt
) {
}