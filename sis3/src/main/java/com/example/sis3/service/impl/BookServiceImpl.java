package com.example.sis3.service.impl;

import com.example.sis3.aspect.Audited;
import com.example.sis3.client.ExternalBookCatalogClient;
import com.example.sis3.config.CacheConfig;
import com.example.sis3.dto.request.CreateBookRequest;
import com.example.sis3.dto.request.UpdateBookRequest;
import com.example.sis3.dto.response.BookResponse;
import com.example.sis3.dto.response.ExternalBookMetadataResponse;
import com.example.sis3.entity.BookEntity;
import com.example.sis3.event.BookEvent;
import com.example.sis3.event.BookEventType;
import com.example.sis3.exception.DuplicateResourceException;
import com.example.sis3.exception.ResourceNotFoundException;
import com.example.sis3.mapper.BookMapper;
import com.example.sis3.model.Book;
import com.example.sis3.repository.BookRepository;
import com.example.sis3.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final ExternalBookCatalogClient externalBookCatalogClient;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    @Audited(action = "CREATE_BOOK")
    @CacheEvict(value = CacheConfig.BOOKS_LIST_CACHE, allEntries = true)
    public BookResponse create(CreateBookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new DuplicateResourceException(
                    "Book with ISBN '" + request.getIsbn() + "' already exists");
        }

        Book domain = bookMapper.toDomain(request);
        BookEntity entity = bookMapper.toEntity(domain);
        BookEntity savedEntity = bookRepository.save(entity);
        publishBookEvent(BookEventType.CREATED, savedEntity);
        Book savedDomain = bookMapper.toDomain(savedEntity);
        return bookMapper.toResponse(savedDomain);
    }

    @Override
    @Cacheable(value = CacheConfig.BOOKS_CACHE, key = "#id")
    public BookResponse getById(Long id) {
        log.debug("Cache MISS for book id={}: querying PostgreSQL database...", id);
        BookEntity entity = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + id + " not found"));
        Book domain = bookMapper.toDomain(entity);
        return bookMapper.toResponse(domain);
    }

    @Override
    @Cacheable(value = CacheConfig.BOOKS_LIST_CACHE)
    public List<BookResponse> getAll() {
        log.debug("Cache MISS for booksList: querying all books from PostgreSQL database...");
        List<BookEntity> entities = bookRepository.findAll();
        List<Book> domainList = entities.stream()
                .map(bookMapper::toDomain)
                .toList();
        return bookMapper.toResponseList(domainList);
    }

    @Override
    public Page<BookResponse> getAllPaged(Pageable pageable) {
        return bookRepository.findAll(pageable)
                .map(bookMapper::toDomain)
                .map(bookMapper::toResponse);
    }

    @Override
    @Transactional
    @Audited(action = "UPDATE_BOOK")
    @CachePut(value = CacheConfig.BOOKS_CACHE, key = "#id")
    @CacheEvict(value = CacheConfig.BOOKS_LIST_CACHE, allEntries = true)
    public BookResponse update(Long id, UpdateBookRequest request) {
        BookEntity existingEntity = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + id + " not found"));

        if (bookRepository.existsByIsbnAndIdNot(request.getIsbn(), id)) {
            throw new DuplicateResourceException(
                    "Another book with ISBN '" + request.getIsbn() + "' already exists");
        }

        Book domain = bookMapper.toDomain(request);
        bookMapper.updateEntityFromDomain(domain, existingEntity);

        BookEntity updatedEntity = bookRepository.save(existingEntity);
        publishBookEvent(BookEventType.UPDATED, updatedEntity);
        Book updatedDomain = bookMapper.toDomain(updatedEntity);
        return bookMapper.toResponse(updatedDomain);
    }

    @Override
    @Transactional
    @Audited(action = "DELETE_BOOK")
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.BOOKS_CACHE, key = "#id"),
            @CacheEvict(value = CacheConfig.BOOKS_LIST_CACHE, allEntries = true)
    })
    public void delete(Long id) {
        BookEntity entity = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + id + " not found"));
        bookRepository.delete(entity);
        publishBookEvent(BookEventType.DELETED, entity);
    }

    private void publishBookEvent(BookEventType type, BookEntity entity) {
        BookEvent event = BookEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(type)
                .bookId(entity.getId())
                .isbn(entity.getIsbn())
                .title(entity.getTitle())
                .author(entity.getAuthor())
                .price(entity.getPrice())
                .occurredAt(Instant.now())
                .build();
        eventPublisher.publishEvent(event);
    }

    @Override
    public ExternalBookMetadataResponse getExternalMetadataByBookId(Long id) {
        BookEntity entity = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + id + " not found"));
        return externalBookCatalogClient.fetchMetadataByIsbn(entity.getIsbn(), entity.getTitle());
    }

    @Override
    public ExternalBookMetadataResponse getExternalMetadataByIsbn(String isbn) {
        return externalBookCatalogClient.fetchMetadataByIsbn(isbn, null);
    }
}
