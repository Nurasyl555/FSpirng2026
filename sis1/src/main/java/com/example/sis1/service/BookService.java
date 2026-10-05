package com.example.sis1.service;

import com.example.sis1.dto.request.CreateBookRequest;
import com.example.sis1.dto.request.UpdateBookRequest;
import com.example.sis1.dto.response.BookResponse;

import java.util.List;

public interface BookService {
    BookResponse create(CreateBookRequest request);
    BookResponse getById(Long id);
    List<BookResponse> getAll();
    BookResponse update(Long id, UpdateBookRequest request);
    void delete(Long id);
}
