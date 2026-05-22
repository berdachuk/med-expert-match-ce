package com.berdachuk.medexpertmatch.documents.rest;

import com.berdachuk.medexpertmatch.documents.DocumentSearchApi;
import com.berdachuk.medexpertmatch.documents.domain.DocumentSearchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Document Search v2", description = "Faceted semantic document search API v2")
@RestController
@Validated
@RequestMapping("/api/v2/documents")
public class DocumentSearchV2Controller {

    private final DocumentSearchApi documentSearchApi;

    public DocumentSearchV2Controller(DocumentSearchApi documentSearchApi) {
        this.documentSearchApi = documentSearchApi;
    }

    @Operation(summary = "Search document chunks with optional facets")
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam @NotBlank @Size(max = 500) String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0.0") double minScore) {
        List<DocumentSearchResult> results = documentSearchApi.searchChunks(query, limit);

        if (category != null && !category.isBlank()) {
            results = results.stream().filter(r -> category.equals(r.category())).toList();
        }
        if (source != null && !source.isBlank()) {
            results = results.stream().filter(r -> source.equals(r.sourceName())).toList();
        }
        if (minScore > 0.0) {
            results = results.stream().filter(r -> r.similarity() >= minScore).toList();
        }

        Map<String, Long> categoryFacet = results.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.category() != null ? r.category() : "unknown",
                        java.util.stream.Collectors.counting()));
        Map<String, Long> sourceFacet = results.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.sourceName() != null ? r.sourceName() : "unknown",
                        java.util.stream.Collectors.counting()));

        return ResponseEntity.ok(Map.of(
                "data", results,
                "facets", Map.of(
                        "categories", categoryFacet,
                        "sources", sourceFacet),
                "meta", Map.of(
                        "version", "2.0",
                        "totalResults", results.size())));
    }
}
