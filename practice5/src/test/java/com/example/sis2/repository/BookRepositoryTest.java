package com.example.sis2.repository;

import com.example.sis2.entity.BookEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Repository Integration Tests for BookRepository against Real Database")
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    @DisplayName("should save and find book by id")
    void shouldSaveAndFindBookById() {
        BookEntity entity = BookEntity.builder()
                .title("Clean Architecture Test")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("39.99"))
                .publicationYear(2017)
                .description("Test description")
                .build();

        BookEntity saved = bookRepository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<BookEntity> found = bookRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Clean Architecture Test");
    }

    @Test
    @DisplayName("existsByIsbn should return true when book exists and false otherwise")
    void shouldCheckIfExistsByIsbn() {
        BookEntity entity = BookEntity.builder()
                .title("Domain-Driven Design Test")
                .author("Eric Evans")
                .isbn("9780321125219")
                .price(new BigDecimal("59.95"))
                .publicationYear(2003)
                .build();

        bookRepository.save(entity);

        assertThat(bookRepository.existsByIsbn("9780321125219")).isTrue();
        assertThat(bookRepository.existsByIsbn("0000000000000")).isFalse();
    }

    @Test
    @DisplayName("existsByIsbnAndIdNot should return true if ISBN belongs to another book")
    void shouldCheckIfExistsByIsbnAndIdNot() {
        BookEntity entity1 = BookEntity.builder()
                .title("Book One")
                .author("Author One")
                .isbn("9781111111111")
                .price(new BigDecimal("29.99"))
                .publicationYear(2021)
                .build();
        BookEntity saved1 = bookRepository.save(entity1);

        BookEntity entity2 = BookEntity.builder()
                .title("Book Two")
                .author("Author Two")
                .isbn("9782222222222")
                .price(new BigDecimal("35.00"))
                .publicationYear(2022)
                .build();
        BookEntity saved2 = bookRepository.save(entity2);

        // Same book, same ISBN -> should be false
        assertThat(bookRepository.existsByIsbnAndIdNot("9781111111111", saved1.getId())).isFalse();

        // Another book exists with this ISBN -> should be true
        assertThat(bookRepository.existsByIsbnAndIdNot("9781111111111", saved2.getId())).isTrue();
    }

    @Test
    @DisplayName("findByIsbn should find the matching book entity")
    void shouldFindByIsbn() {
        BookEntity entity = BookEntity.builder()
                .title("Refactoring Test")
                .author("Martin Fowler")
                .isbn("9780201485677")
                .price(new BigDecimal("47.99"))
                .publicationYear(1999)
                .build();
        bookRepository.save(entity);

        Optional<BookEntity> found = bookRepository.findByIsbn("9780201485677");
        assertThat(found).isPresent();
        assertThat(found.get().getAuthor()).isEqualTo("Martin Fowler");
    }
}
