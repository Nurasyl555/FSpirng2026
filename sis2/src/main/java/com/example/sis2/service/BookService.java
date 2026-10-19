package com.example.sis2.service;

import com.example.sis2.dto.request.CreateBookRequest;
import com.example.sis2.dto.request.UpdateBookRequest;
import com.example.sis2.dto.response.BookResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookService {

    BookResponse create(CreateBookRequest request);

    BookResponse getById(Long id);

    List<BookResponse> getAll();

    Page<BookResponse> getAllPaged(Pageable pageable);

    BookResponse update(Long id, UpdateBookRequest request);

    void delete(Long id);
}
