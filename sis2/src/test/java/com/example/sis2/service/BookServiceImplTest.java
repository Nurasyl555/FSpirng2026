package com.example.sis2.service;

import com.example.sis2.dto.request.CreateBookRequest;
import com.example.sis2.dto.request.UpdateBookRequest;
import com.example.sis2.dto.response.BookResponse;
import com.example.sis2.entity.BookEntity;
import com.example.sis2.exception.DuplicateResourceException;
import com.example.sis2.exception.ResourceNotFoundException;
import com.example.sis2.mapper.BookMapper;
import com.example.sis2.model.Book;
import com.example.sis2.repository.BookRepository;
import com.example.sis2.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for BookServiceImpl")
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    private CreateBookRequest createRequest;
    private UpdateBookRequest updateRequest;
    private Book domainBook;
    private BookEntity bookEntity;
    private BookResponse bookResponse;

    @BeforeEach
    void setUp() {
        createRequest = CreateBookRequest.builder()
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("39.99"))
                .publicationYear(2017)
                .description("A Craftsman's Guide to Software Structure and Design")
                .build();

        updateRequest = UpdateBookRequest.builder()
                .title("Clean Architecture Updated")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("44.99"))
                .publicationYear(2018)
                .description("Updated Edition")
                .build();

        domainBook = Book.builder()
                .id(1L)
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("39.99"))
                .publicationYear(2017)
                .description("A Craftsman's Guide to Software Structure and Design")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        bookEntity = BookEntity.builder()
                .id(1L)
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("39.99"))
                .publicationYear(2017)
                .description("A Craftsman's Guide to Software Structure and Design")
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
                .description("A Craftsman's Guide to Software Structure and Design")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("create() should successfully save book and return response when ISBN is unique")
    void create_WhenValidRequest_ShouldSaveAndReturnResponse() {
        when(bookRepository.existsByIsbn(createRequest.getIsbn())).thenReturn(false);
        when(bookMapper.toDomain(createRequest)).thenReturn(domainBook);
        when(bookMapper.toEntity(domainBook)).thenReturn(bookEntity);
        when(bookRepository.save(bookEntity)).thenReturn(bookEntity);
        when(bookMapper.toDomain(bookEntity)).thenReturn(domainBook);
        when(bookMapper.toResponse(domainBook)).thenReturn(bookResponse);

        BookResponse result = bookService.create(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Clean Architecture");
        assertThat(result.getIsbn()).isEqualTo("9780134494166");

        verify(bookRepository).existsByIsbn(createRequest.getIsbn());
        verify(bookRepository).save(bookEntity);
    }

    @Test
    @DisplayName("create() should throw DuplicateResourceException when ISBN already exists")
    void create_WhenDuplicateIsbn_ShouldThrowDuplicateResourceException() {
        when(bookRepository.existsByIsbn(createRequest.getIsbn())).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("9780134494166");

        verify(bookRepository).existsByIsbn(createRequest.getIsbn());
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("getById() should return BookResponse when book is found")
    void getById_WhenBookExists_ShouldReturnResponse() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(bookEntity));
        when(bookMapper.toDomain(bookEntity)).thenReturn(domainBook);
        when(bookMapper.toResponse(domainBook)).thenReturn(bookResponse);

        BookResponse result = bookService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Clean Architecture");

        verify(bookRepository).findById(1L);
    }

    @Test
    @DisplayName("getById() should throw ResourceNotFoundException when book does not exist")
    void getById_WhenBookNotFound_ShouldThrowResourceNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("id 99 not found");

        verify(bookRepository).findById(99L);
    }

    @Test
    @DisplayName("getAll() should return list of BookResponse objects")
    void getAll_ShouldReturnListOfBookResponses() {
        when(bookRepository.findAll()).thenReturn(List.of(bookEntity));
        when(bookMapper.toDomain(bookEntity)).thenReturn(domainBook);
        when(bookMapper.toResponseList(List.of(domainBook))).thenReturn(List.of(bookResponse));

        List<BookResponse> result = bookService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Clean Architecture");

        verify(bookRepository).findAll();
    }

    @Test
    @DisplayName("getAllPaged() should return paginated BookResponse objects")
    void getAllPaged_ShouldReturnPageOfBookResponses() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BookEntity> entityPage = new PageImpl<>(List.of(bookEntity), pageable, 1);

        when(bookRepository.findAll(pageable)).thenReturn(entityPage);
        when(bookMapper.toDomain(bookEntity)).thenReturn(domainBook);
        when(bookMapper.toResponse(domainBook)).thenReturn(bookResponse);

        Page<BookResponse> result = bookService.getAllPaged(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Architecture");

        verify(bookRepository).findAll(pageable);
    }

    @Test
    @DisplayName("update() should update existing book and return response when ISBN is unique")
    void update_WhenValidRequest_ShouldUpdateAndReturnResponse() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(bookEntity));
        when(bookRepository.existsByIsbnAndIdNot(updateRequest.getIsbn(), 1L)).thenReturn(false);
        when(bookMapper.toDomain(updateRequest)).thenReturn(domainBook);
        when(bookRepository.save(bookEntity)).thenReturn(bookEntity);
        when(bookMapper.toDomain(bookEntity)).thenReturn(domainBook);
        when(bookMapper.toResponse(domainBook)).thenReturn(bookResponse);

        BookResponse result = bookService.update(1L, updateRequest);

        assertThat(result).isNotNull();
        verify(bookRepository).findById(1L);
        verify(bookRepository).existsByIsbnAndIdNot(updateRequest.getIsbn(), 1L);
        verify(bookMapper).updateEntityFromDomain(domainBook, bookEntity);
        verify(bookRepository).save(bookEntity);
    }

    @Test
    @DisplayName("update() should throw ResourceNotFoundException when book does not exist")
    void update_WhenBookNotFound_ShouldThrowResourceNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update(99L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("id 99 not found");

        verify(bookRepository).findById(99L);
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("update() should throw DuplicateResourceException when ISBN belongs to another book")
    void update_WhenDuplicateIsbnOnDifferentBook_ShouldThrowDuplicateResourceException() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(bookEntity));
        when(bookRepository.existsByIsbnAndIdNot(updateRequest.getIsbn(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> bookService.update(1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(bookRepository).findById(1L);
        verify(bookRepository).existsByIsbnAndIdNot(updateRequest.getIsbn(), 1L);
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete() should delete existing book by id")
    void delete_WhenBookExists_ShouldDeleteSuccessfully() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(bookEntity));

        bookService.delete(1L);

        verify(bookRepository).findById(1L);
        verify(bookRepository).delete(bookEntity);
    }

    @Test
    @DisplayName("delete() should throw ResourceNotFoundException when book does not exist")
    void delete_WhenBookNotFound_ShouldThrowResourceNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("id 99 not found");

        verify(bookRepository).findById(99L);
        verify(bookRepository, never()).delete(any());
    }
}
