package com.berdachuk.medexpertmatch.documents.rest;

import com.berdachuk.medexpertmatch.documents.DocumentSearchApi;
import com.berdachuk.medexpertmatch.documents.domain.DocumentSearchResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentSearchController {

    private final DocumentSearchApi documentSearchApi;

    public DocumentSearchController(DocumentSearchApi documentSearchApi) {
        this.documentSearchApi = documentSearchApi;
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentSearchResult>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit,
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
