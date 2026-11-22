package com.example.sis2.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating a new book in the catalog")
public class CreateBookRequest {

    @Schema(description = "Title of the book", example = "Clean Code")
    @NotBlank(message = "Title is required and cannot be blank")
    @Size(min = 2, max = 150, message = "Title must be between 2 and 150 characters")
    private String title;

    @Schema(description = "Full name of the author", example = "Robert C. Martin")
    @NotBlank(message = "Author is required and cannot be blank")
    @Size(min = 2, max = 100, message = "Author must be between 2 and 100 characters")
    private String author;

    @Schema(description = "Unique 10 or 13 digit ISBN identifier", example = "9780132350884")
    @NotBlank(message = "ISBN is required and cannot be blank")
    @Pattern(
        regexp = "^(97[89])?[0-9]{9}[0-9X]$",
        message = "ISBN must be a valid 10 or 13 digit format (e.g., 9780132350884 or 0132350882)"
    )
    private String isbn;

    @Schema(description = "Retail price of the book in USD", example = "39.99")
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal price;

    @Schema(description = "Year the book was published", example = "2008")
    @NotNull(message = "Publication year is required")
    @Min(value = 1450, message = "Publication year must be after 1450")
    @Max(value = 2026, message = "Publication year cannot be in the future")
    private Integer publicationYear;

    @Schema(description = "Short synopsis or overview of the book",
            example = "A Handbook of Agile Software Craftsmanship")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
