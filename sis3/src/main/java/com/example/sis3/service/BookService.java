package com.example.sis3.service;

import com.example.sis3.dto.request.CreateBookRequest;
import com.example.sis3.dto.request.UpdateBookRequest;
import com.example.sis3.dto.response.BookResponse;
import com.example.sis3.dto.response.ExternalBookMetadataResponse;
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

    ExternalBookMetadataResponse getExternalMetadataByBookId(Long id);

    ExternalBookMetadataResponse getExternalMetadataByIsbn(String isbn);
}
