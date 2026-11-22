package com.example.sis2.controller;

import com.example.sis2.dto.request.CreateBookRequest;
import com.example.sis2.dto.request.UpdateBookRequest;
import com.example.sis2.dto.response.BookResponse;
import com.example.sis2.dto.response.ErrorResponse;
import com.example.sis2.dto.response.ExternalBookMetadataResponse;
import com.example.sis2.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "CRUD operations for the Bookstore catalog and external OpenLibrary metadata enrichment")
public class BookController {

    private final BookService bookService;

    @Operation(
            summary = "Create a new book",
            description = "Validates the request payload, persists a new book in PostgreSQL, and evicts the catalog list cache."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book successfully created",
                    content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed for request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Book with the given ISBN already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        BookResponse createdBook = bookService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBook.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdBook);
    }

    @Operation(
            summary = "Get paginated books",
            description = "Returns a paginated and sorted page of books from PostgreSQL."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of books returned successfully")
    })
    @GetMapping
    public ResponseEntity<Page<BookResponse>> getAllBooks(
            @ParameterObject
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(bookService.getAllPaged(pageable));
    }

    @Operation(
            summary = "Get full cached book catalog",
            description = "Read-heavy endpoint backed by the 'booksList' Caffeine cache."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Full catalog list returned")
    })
    @GetMapping("/catalog")
    public ResponseEntity<List<BookResponse>> getCatalog() {
        return ResponseEntity.ok(bookService.getAll());
    }

    @Operation(
            summary = "Get a book by ID",
            description = "Retrieves a single book by its database ID (cached in the 'books' Caffeine cache)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book found and returned",
                    content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book with the specified ID was not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(
            @Parameter(description = "Database ID of the book", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(bookService.getById(id));
    }

    @Operation(
            summary = "Update an existing book",
            description = "Updates book details by ID, refreshes the 'books' cache entry, and evicts the 'booksList' cache."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book updated successfully",
                    content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error in request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book with the specified ID was not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Another book with the same ISBN already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(
            @Parameter(description = "Database ID of the book to update", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookRequest request) {
        return ResponseEntity.ok(bookService.update(id, request));
    }

    @Operation(
            summary = "Delete a book by ID",
            description = "Deletes the book from PostgreSQL and evicts its entries from Caffeine caches."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Book with the specified ID was not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteBook(
            @Parameter(description = "Database ID of the book to delete", example = "1")
            @PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Fetch external OpenLibrary metadata for a stored book by ID",
            description = "Looks up the book in PostgreSQL by ID, then calls the external OpenLibrary REST API "
                    + "using its ISBN with configured connect/read timeouts. Returns fallback data if the external service fails or times out."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "External metadata (or resilient fallback) returned",
                    content = @Content(schema = @Schema(implementation = ExternalBookMetadataResponse.class))),
            @ApiResponse(responseCode = "404", description = "Local book with the specified ID was not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/external-metadata")
    public ResponseEntity<ExternalBookMetadataResponse> getExternalMetadataByBookId(
            @Parameter(description = "Database ID of the book", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(bookService.getExternalMetadataByBookId(id));
    }

    @Operation(
            summary = "Fetch external OpenLibrary metadata directly by ISBN",
            description = "Calls the external OpenLibrary REST API for any ISBN with configured connect/read timeouts "
                    + "and returns a fallback response if the external service is unreachable or times out."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "External metadata (or resilient fallback) returned",
                    content = @Content(schema = @Schema(implementation = ExternalBookMetadataResponse.class)))
    })
    @GetMapping("/external/isbn/{isbn}")
    public ResponseEntity<ExternalBookMetadataResponse> getExternalMetadataByIsbn(
            @Parameter(description = "10 or 13 digit ISBN to look up in OpenLibrary", example = "9780132350884")
            @PathVariable String isbn) {
        return ResponseEntity.ok(bookService.getExternalMetadataByIsbn(isbn));
    }
}
