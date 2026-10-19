package com.example.sis2.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Pure domain model representing a Book in the business logic layer.
 * Decoupled from persistence concerns and annotations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private BigDecimal price;
    private Integer publicationYear;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
