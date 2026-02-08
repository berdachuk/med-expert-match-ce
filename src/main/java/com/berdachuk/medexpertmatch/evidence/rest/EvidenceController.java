package com.berdachuk.medexpertmatch.evidence.rest;

import com.berdachuk.medexpertmatch.evidence.domain.PubMedArticle;
import com.berdachuk.medexpertmatch.evidence.service.PubMedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for evidence services (PubMed, guidelines).
 * Provides verification endpoints for tools used in case analysis.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/evidence")
public class EvidenceController {

    private final PubMedService pubmedService;

    public EvidenceController(PubMedService pubmedService) {
        this.pubmedService = pubmedService;
    }

    /**
     * Verifies the PubMed tool by running a search and returning article count and sample.
     * Use this to confirm PubMed is reachable from the app server and parsing works.
     *
     * @param query      Search query (default: GERD)
     * @param maxResults Max articles to fetch (default: 3)
     * @return JSON with status, articleCount, query, and sample titles/PMIDs
     */
    @GetMapping("/verify-pubmed")
    public ResponseEntity<Map<String, Object>> verifyPubMed(
            @RequestParam(defaultValue = "GERD") String query,
            @RequestParam(defaultValue = "3") int maxResults
    ) {
        log.info("GET /api/v1/evidence/verify-pubmed query={}, maxResults={}", query, maxResults);

        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("maxResults", maxResults);

        try {
            List<PubMedArticle> articles = pubmedService.search(query, Math.min(maxResults, 10));
            body.put("status", "ok");
            body.put("articleCount", articles.size());
            body.put("message", articles.isEmpty()
                    ? "PubMed returned 0 articles (check network from app server or try another query)"
                    : "PubMed tool verified successfully");

            if (!articles.isEmpty()) {
                List<Map<String, String>> sample = articles.stream()
                        .map(a -> {
                            Map<String, String> m = new HashMap<>();
                            m.put("pmid", a.pmid());
                            m.put("title", a.title() != null ? a.title() : "");
                            return m;
                        })
                        .collect(Collectors.toList());
                body.put("sample", sample);
            }

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("PubMed verification failed", e);
            body.put("status", "error");
            body.put("articleCount", 0);
            body.put("message", "PubMed request failed: " + e.getMessage());
            return ResponseEntity.status(500).body(body);
        }
    }
}
