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

import java.util.List;
import java.util.Map;

@Tag(name = "Document Search", description = "Semantic document search API")
@RestController
@Validated
@RequestMapping("/api/v1/documents")
public class DocumentSearchController {

    private final DocumentSearchApi documentSearchApi;

    public DocumentSearchController(DocumentSearchApi documentSearchApi) {
        this.documentSearchApi = documentSearchApi;
    }

    @Operation(summary = "Search document chunks by semantic similarity")
    @GetMapping("/search")
    public ResponseEntity<List<DocumentSearchResult>> search(
            @RequestParam @NotBlank @Size(max = 500) String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0.0") double minScore) {
        List<DocumentSearchResult> results = documentSearchApi.searchChunks(query, limit);
        if (minScore > 0.0) {
            results = results.stream().filter(r -> r.similarity() >= minScore).toList();
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/health")
    public ResponseEntity<Map<String, Object>> searchHealth() {
        try {
            documentSearchApi.searchChunks("test", 1);
            return ResponseEntity.ok(Map.of("status", "UP"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", "DOWN", "error", e.getMessage()));
        }
    }
}
