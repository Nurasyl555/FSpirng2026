package com.example.sis2.controller;

import com.example.sis2.dto.request.CreateBookRequest;
import com.example.sis2.dto.request.UpdateBookRequest;
import com.example.sis2.dto.response.BookResponse;
import com.example.sis2.exception.DuplicateResourceException;
import com.example.sis2.exception.GlobalExceptionHandler;
import com.example.sis2.exception.ResourceNotFoundException;
import com.example.sis2.service.BookService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("MockMvc Slice Tests for BookController")
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService bookService;

    private BookResponse sampleBookResponse;

    @BeforeEach
    void setUp() {
        sampleBookResponse = BookResponse.builder()
                .id(1L)
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("39.99"))
                .publicationYear(2017)
                .description("Architecture handbook")
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/books should return 201 Created and Location header on valid request")
    void createBook_WhenValid_ShouldReturn201() throws Exception {
        CreateBookRequest request = CreateBookRequest.builder()
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("39.99"))
                .publicationYear(2017)
                .description("Architecture handbook")
                .build();

        when(bookService.create(any(CreateBookRequest.class))).thenReturn(sampleBookResponse);

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/books/1")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Clean Architecture")))
                .andExpect(jsonPath("$.isbn", is("9780134494166")))
                .andExpect(jsonPath("$.price", is(39.99)));
    }

    @Test
    @DisplayName("POST /api/v1/books should return 400 Bad Request when title is blank")
    void createBook_WhenTitleBlank_ShouldReturn400() throws Exception {
        CreateBookRequest request = CreateBookRequest.builder()
                .title("")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("39.99"))
                .publicationYear(2017)
                .build();

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/books should return 400 Bad Request when price is negative or zero")
    void createBook_WhenPriceZeroOrNegative_ShouldReturn400() throws Exception {
        CreateBookRequest request = CreateBookRequest.builder()
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("-5.00"))
                .publicationYear(2017)
                .build();

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors[0].field", is("price")));
    }

    @Test
    @DisplayName("POST /api/v1/books should return 400 Bad Request when ISBN pattern is invalid")
    void createBook_WhenIsbnInvalid_ShouldReturn400() throws Exception {
        CreateBookRequest request = CreateBookRequest.builder()
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .isbn("invalid-isbn")
                .price(new BigDecimal("39.99"))
                .publicationYear(2017)
                .build();

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors[0].field", is("isbn")));
    }

    @Test
    @DisplayName("POST /api/v1/books should return 409 Conflict when duplicate ISBN exception is thrown")
    void createBook_WhenDuplicateIsbn_ShouldReturn409() throws Exception {
        CreateBookRequest request = CreateBookRequest.builder()
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("39.99"))
                .publicationYear(2017)
                .build();

        when(bookService.create(any(CreateBookRequest.class)))
                .thenThrow(new DuplicateResourceException("Book with ISBN '9780134494166' already exists"));

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("GET /api/v1/books/{id} should return 200 OK when book is found")
    void getBookById_WhenExists_ShouldReturn200() throws Exception {
        when(bookService.getById(1L)).thenReturn(sampleBookResponse);

        mockMvc.perform(get("/api/v1/books/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Clean Architecture")))
                .andExpect(jsonPath("$.author", is("Robert C. Martin")));
    }

    @Test
    @DisplayName("GET /api/v1/books/{id} should return 404 Not Found when book does not exist")
    void getBookById_WhenNotFound_ShouldReturn404() throws Exception {
        when(bookService.getById(99L)).thenThrow(new ResourceNotFoundException("Book with id 99 not found"));

        mockMvc.perform(get("/api/v1/books/99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("id 99 not found")));
    }

    @Test
    @DisplayName("GET /api/v1/books should return 200 OK with paginated list")
    void getAllBooks_ShouldReturn200WithPage() throws Exception {
        PageImpl<BookResponse> page = new PageImpl<>(List.of(sampleBookResponse), PageRequest.of(0, 10), 1);
        when(bookService.getAllPaged(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/books?page=0&size=10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Clean Architecture")));
    }

    @Test
    @DisplayName("PUT /api/v1/books/{id} should return 200 OK on valid update")
    void updateBook_WhenValid_ShouldReturn200() throws Exception {
        UpdateBookRequest request = UpdateBookRequest.builder()
                .title("Clean Architecture 2nd Edition")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("49.99"))
                .publicationYear(2020)
                .build();

        BookResponse updatedResponse = BookResponse.builder()
                .id(1L)
                .title("Clean Architecture 2nd Edition")
                .author("Robert C. Martin")
                .isbn("9780134494166")
                .price(new BigDecimal("49.99"))
                .publicationYear(2020)
                .build();

        when(bookService.update(eq(1L), any(UpdateBookRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Clean Architecture 2nd Edition")))
                .andExpect(jsonPath("$.price", is(49.99)));
    }

    @Test
    @DisplayName("DELETE /api/v1/books/{id} should return 204 No Content on successful deletion")
    void deleteBook_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(bookService).delete(1L);

        mockMvc.perform(delete("/api/v1/books/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/books/{id} should return 404 Not Found when book to delete does not exist")
    void deleteBook_WhenNotFound_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Book with id 99 not found")).when(bookService).delete(99L);

        mockMvc.perform(delete("/api/v1/books/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}
