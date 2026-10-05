package com.example.sis1.service.impl;

import com.example.sis1.dto.request.CreateBookRequest;
import com.example.sis1.dto.request.UpdateBookRequest;
import com.example.sis1.dto.response.BookResponse;
import com.example.sis1.exception.DuplicateResourceException;
import com.example.sis1.exception.ResourceNotFoundException;
import com.example.sis1.model.Book;
import com.example.sis1.repository.BookRepository;
import com.example.sis1.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    @Override
    public BookResponse create(CreateBookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new DuplicateResourceException(
                    "Book with ISBN '" + request.getIsbn() + "' already exists");
        }

        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .price(request.getPrice())
                .publicationYear(request.getPublicationYear())
                .description(request.getDescription())
                .build();

        Book savedBook = bookRepository.save(book);
        return mapToResponse(savedBook);
    }

    @Override
    public BookResponse getById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + id + " not found"));
        return mapToResponse(book);
    }

    @Override
    public List<BookResponse> getAll() {
        return bookRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BookResponse update(Long id, UpdateBookRequest request) {
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + id + " not found"));

        if (bookRepository.existsByIsbnAndIdNot(request.getIsbn(), id)) {
            throw new DuplicateResourceException(
                    "Another book with ISBN '" + request.getIsbn() + "' already exists");
        }

        existingBook.setTitle(request.getTitle());
        existingBook.setAuthor(request.getAuthor());
        existingBook.setIsbn(request.getIsbn());
        existingBook.setPrice(request.getPrice());
        existingBook.setPublicationYear(request.getPublicationYear());
        existingBook.setDescription(request.getDescription());

        Book updatedBook = bookRepository.save(existingBook);
        return mapToResponse(updatedBook);
    }

    @Override
    public void delete(Long id) {
        if (!bookRepository.deleteById(id)) {
            throw new ResourceNotFoundException("Book with id " + id + " not found");
        }
    }

    private BookResponse mapToResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .price(book.getPrice())
                .publicationYear(book.getPublicationYear())
                .description(book.getDescription())
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }
}
