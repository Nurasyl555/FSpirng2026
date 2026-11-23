package com.example.sis3.messaging;

import com.example.sis3.event.BookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BookDomainEventListener {

    private final BookEventProducer bookEventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookEvent(BookEvent event) {
        bookEventProducer.send(event);
    }
}