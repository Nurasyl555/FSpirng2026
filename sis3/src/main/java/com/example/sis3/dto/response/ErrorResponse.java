package com.example.sis3.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Unified error response structure returned on 4xx and 5xx HTTP errors")
public class ErrorResponse {

    @Schema(description = "Timestamp when the error occurred", example = "2026-11-09T09:30:00Z")
    private Instant timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "HTTP status reason phrase", example = "Not Found")
    private String error;

    @Schema(description = "Human-readable error message", example = "Book with id 99 not found")
    private String message;

    @Schema(description = "Request URI path that triggered the error", example = "/api/v1/books/99")
    private String path;

    @Schema(description = "Field-level validation errors (present on 400 Bad Request)")
    private List<ValidationError> validationErrors;
}
