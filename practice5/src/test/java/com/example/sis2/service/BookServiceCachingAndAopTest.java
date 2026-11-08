package com.example.sis2.service;

import com.example.sis2.aspect.ServiceLoggingAndTimingAspect;
import com.example.sis2.config.CacheConfig;
import com.example.sis2.dto.request.CreateBookRequest;
import com.example.sis2.dto.request.UpdateBookRequest;
import com.example.sis2.dto.response.BookResponse;
import com.example.sis2.entity.BookEntity;
import com.example.sis2.mapper.BookMapper;
import com.example.sis2.model.Book;
import com.example.sis2.repository.BookRepository;
import com.example.sis2.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
@Import({
        AopAutoConfiguration.class,
        CacheConfig.class,
        ServiceLoggingAndTimingAspect.class,
        BookServiceImpl.class
})
@DisplayName("Practice 5: AOP Logging/Timing Aspect & Caching Tests")
class BookServiceCachingAndAopTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private BookMapper bookMapper;

    private BookEntity bookEntity;
    private Book domainBook;
    private BookResponse bookResponse;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });

        bookEntity = BookEntity.builder()
                .id(1L)
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("39.99"))
                .publicationYear(2017)
                .description("Software Architecture Guide")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        domainBook = Book.builder()
                .id(1L)
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("39.99"))
                .publicationYear(2017)
                .description("Software Architecture Guide")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        bookResponse = BookResponse.builder()
                .id(1L)
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("39.99"))
                .publicationYear(2017)
                .description("Software Architecture Guide")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getById() should cache result on first call and skip DB query on second call")
    void getById_ShouldUseCacheOnRepeatedCalls() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(bookEntity));
        when(bookMapper.toDomain(bookEntity)).thenReturn(domainBook);
        when(bookMapper.toResponse(domainBook)).thenReturn(bookResponse);

        // First call -> Cache MISS (hits repository)
        BookResponse firstCall = bookService.getById(1L);
        // Second call -> Cache HIT (skips repository)
        BookResponse secondCall = bookService.getById(1L);

        assertThat(firstCall).isEqualTo(secondCall);
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getAll() should cache catalog list and evict when a new book is created")
    void getAll_ShouldCacheAndEvictOnCreate() {
        when(bookRepository.findAll()).thenReturn(List.of(bookEntity));
        when(bookMapper.toDomain(bookEntity)).thenReturn(domainBook);
        when(bookMapper.toResponseList(List.of(domainBook))).thenReturn(List.of(bookResponse));

        // 1st and 2nd calls -> only 1 DB call
        bookService.getAll();
        bookService.getAll();
        verify(bookRepository, times(1)).findAll();

        // Create a new book -> evicts booksList cache
        CreateBookRequest createRequest = CreateBookRequest.builder()
                .title("Spring in Action")
                .author("Craig Walls")
                .isbn("9781617294945")
                .price(new BigDecimal("49.99"))
                .publicationYear(2018)
                .build();

        when(bookRepository.existsByIsbn(createRequest.getIsbn())).thenReturn(false);
        when(bookMapper.toDomain(createRequest)).thenReturn(domainBook);
        when(bookMapper.toEntity(domainBook)).thenReturn(bookEntity);
        when(bookRepository.save(any())).thenReturn(bookEntity);

        bookService.create(createRequest);

        // Next getAll() call should hit DB again after cache eviction
        bookService.getAll();
        verify(bookRepository, times(2)).findAll();
    }

    @Test
    @DisplayName("update() should update the cached book entry via @CachePut")
    void update_ShouldRefreshCacheEntry() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(bookEntity));
        when(bookMapper.toDomain(bookEntity)).thenReturn(domainBook);
        when(bookMapper.toResponse(domainBook)).thenReturn(bookResponse);

        // Populate cache
        bookService.getById(1L);

        UpdateBookRequest updateRequest = UpdateBookRequest.builder()
                .title("Clean Architecture Revised")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("45.00"))
                .publicationYear(2018)
                .build();

        BookResponse updatedResponse = BookResponse.builder()
                .id(1L)
                .title("Clean Architecture Revised")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("45.00"))
                .publicationYear(2018)
                .build();

        when(bookRepository.existsByIsbnAndIdNot(updateRequest.getIsbn(), 1L)).thenReturn(false);
        when(bookMapper.toDomain(updateRequest)).thenReturn(domainBook);
        when(bookRepository.save(bookEntity)).thenReturn(bookEntity);
        when(bookMapper.toResponse(domainBook)).thenReturn(updatedResponse);

        bookService.update(1L, updateRequest);

        // Subsequent getById(1L) should return the updated value from cache without an extra findById call
        BookResponse fromCache = bookService.getById(1L);
        assertThat(fromCache.getTitle()).isEqualTo("Clean Architecture Revised");
        // 1 from initial getById + 1 inside update() = 2 total findById calls (no 3rd call!)
        verify(bookRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("delete() should evict the book from cache via @CacheEvict")
    void delete_ShouldEvictBookFromCache() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(bookEntity));
        when(bookMapper.toDomain(bookEntity)).thenReturn(domainBook);
        when(bookMapper.toResponse(domainBook)).thenReturn(bookResponse);

        // Populate cache
        bookService.getById(1L);
        assertThat(cacheManager.getCache(CacheConfig.BOOKS_CACHE).get(1L)).isNotNull();

        // Delete book -> evicts key 1L
        bookService.delete(1L);
        assertThat(cacheManager.getCache(CacheConfig.BOOKS_CACHE).get(1L)).isNull();
    }
}
