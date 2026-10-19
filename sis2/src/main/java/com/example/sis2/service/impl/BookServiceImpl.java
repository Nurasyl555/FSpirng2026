package com.example.sis2.service.impl;

import com.example.sis2.dto.request.CreateBookRequest;
import com.example.sis2.dto.request.UpdateBookRequest;
import com.example.sis2.dto.response.BookResponse;
import com.example.sis2.entity.BookEntity;
import com.example.sis2.exception.DuplicateResourceException;
import com.example.sis2.exception.ResourceNotFoundException;
import com.example.sis2.mapper.BookMapper;
import com.example.sis2.model.Book;
import com.example.sis2.repository.BookRepository;
import com.example.sis2.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    @Override
    @Transactional
    public BookResponse create(CreateBookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new DuplicateResourceException(
                    "Book with ISBN '" + request.getIsbn() + "' already exists");
        }

        Book domain = bookMapper.toDomain(request);
        BookEntity entity = bookMapper.toEntity(domain);
        BookEntity savedEntity = bookRepository.save(entity);
        Book savedDomain = bookMapper.toDomain(savedEntity);
        return bookMapper.toResponse(savedDomain);
    }

    @Override
    public BookResponse getById(Long id) {
        BookEntity entity = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + id + " not found"));
        Book domain = bookMapper.toDomain(entity);
        return bookMapper.toResponse(domain);
    }

    @Override
    public List<BookResponse> getAll() {
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
        Book updatedDomain = bookMapper.toDomain(updatedEntity);
        return bookMapper.toResponse(updatedDomain);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BookEntity entity = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + id + " not found"));
        bookRepository.delete(entity);
    }
}
