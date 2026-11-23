package com.example.sis3.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Book resource returned by the Bookstore API")
public class BookResponse {

    @Schema(description = "Unique database identifier of the book", example = "1")
    private Long id;

    @Schema(description = "Title of the book", example = "Clean Code")
    private String title;

    @Schema(description = "Author of the book", example = "Robert C. Martin")
    private String author;

    @Schema(description = "10 or 13 digit ISBN identifier", example = "9780132350884")
    private String isbn;

    @Schema(description = "Price in USD", example = "39.99")
    private BigDecimal price;

    @Schema(description = "Year of publication", example = "2008")
    private Integer publicationYear;

    @Schema(description = "Book synopsis or description", example = "A Handbook of Agile Software Craftsmanship")
    private String description;

    @Schema(description = "Timestamp when the record was created", example = "2026-01-01T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Timestamp when the record was last updated", example = "2026-01-01T10:00:00Z")
    private Instant updatedAt;
}
