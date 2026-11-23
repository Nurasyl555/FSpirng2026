package com.example.sis3.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Enriched book metadata fetched from the external OpenLibrary service (or fallback when unavailable)")
public class ExternalBookMetadataResponse {

    @Schema(description = "ISBN-10 or ISBN-13 used to query the external catalog", example = "9780132350884")
    private String isbn;

    @Schema(description = "Book title from external catalog or local fallback", example = "Clean Code")
    private String title;

    @Schema(description = "Primary publisher returned by external service", example = "Prentice Hall")
    private String publisher;

    @Schema(description = "Number of pages reported by external catalog", example = "464")
    private Integer numberOfPages;

    @Schema(description = "External catalog information page URL", example = "https://openlibrary.org/isbn/9780132350884")
    private String previewUrl;

    @Schema(description = "Source of the returned data: OPEN_LIBRARY_API or FALLBACK_LOCAL", example = "OPEN_LIBRARY_API")
    private String source;

    @Schema(description = "True if the external call failed/timed out and fallback data was returned", example = "false")
    private boolean fallback;

    @Schema(description = "Diagnostic message explaining live fetch or fallback reason",
            example = "Successfully fetched metadata from OpenLibrary API")
    private String message;

    @Schema(description = "Timestamp when the metadata was retrieved", example = "2026-11-09T09:30:00Z")
    private Instant retrievedAt;
}
