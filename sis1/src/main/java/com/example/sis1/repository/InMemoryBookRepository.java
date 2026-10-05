package com.example.sis1.repository;

import com.example.sis1.model.Book;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryBookRepository implements BookRepository {

    private final Map<Long, Book> storage = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public Book save(Book book) {
        Instant now = Instant.now();
        if (book.getId() == null) {
            book.setId(idSequence.getAndIncrement());
            book.setCreatedAt(now);
            book.setUpdatedAt(now);
        } else {
            book.setUpdatedAt(now);
        }
        storage.put(book.getId(), book);
        return book;
    }

    @Override
    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Book> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean existsByIsbn(String isbn) {
        return storage.values().stream()
                .anyMatch(book -> book.getIsbn() != null && book.getIsbn().equalsIgnoreCase(isbn));
    }

    @Override
    public boolean existsByIsbnAndIdNot(String isbn, Long id) {
        return storage.values().stream()
                .anyMatch(book -> book.getIsbn() != null 
                        && book.getIsbn().equalsIgnoreCase(isbn) 
                        && !book.getId().equals(id));
    }

    @Override
    public boolean deleteById(Long id) {
        return storage.remove(id) != null;
    }

    @Override
    public void clear() {
        storage.clear();
        idSequence.set(1);
    }
}
