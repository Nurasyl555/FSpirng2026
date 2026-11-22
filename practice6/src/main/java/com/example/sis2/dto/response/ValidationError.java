package com.example.sis2.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of a single field validation error")
public class ValidationError {

    @Schema(description = "Name of the invalid field", example = "isbn")
    private String field;

    @Schema(description = "Value that was rejected by validation", example = "invalid-isbn")
    private Object rejectedValue;

    @Schema(description = "Validation failure message",
            example = "ISBN must be a valid 10 or 13 digit format (e.g., 9780132350884 or 0132350882)")
    private String message;
}
