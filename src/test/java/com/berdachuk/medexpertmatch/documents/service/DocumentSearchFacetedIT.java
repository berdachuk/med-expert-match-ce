package com.berdachuk.medexpertmatch.documents.service;

import com.berdachuk.medexpertmatch.documents.DocumentIngestApi;
import com.berdachuk.medexpertmatch.documents.DocumentSearchApi;
import com.berdachuk.medexpertmatch.documents.domain.DocumentSearchFilters;
import com.berdachuk.medexpertmatch.documents.domain.DocumentSearchResult;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REQ-016: integration coverage for registered requirement.
 */
class DocumentSearchFacetedIT extends BaseIntegrationTest {

    @Autowired
    private DocumentIngestApi documentIngestApi;

    @Autowired
    private DocumentSearchApi documentSearchApi;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.document_chunk");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.source_document");
    }

    private static String uniqueText(String topic) {
        return "Clinical evidence document about " + topic + " with detailed treatment guidance "
                + "for semantic search validation across medical specialties and care pathways.";
    }

    @Test
    void shouldFilterSearchResultsByCategoryInSql() throws Exception {
        Path jsonl = tempDir.resolve("faceted.jsonl");
        Files.writeString(jsonl,
                "{\"id\": \"f-1\", \"title\": \"Clinical Doc\", \"category\": \"clinical\", \"source\": \"WHO\", \"text\": \"" + uniqueText("cardiology") + "\"}\n"
                        + "{\"id\": \"f-2\", \"title\": \"Research Doc\", \"category\": \"research\", \"source\": \"PubMed\", \"text\": \"" + uniqueText("oncology") + "\"}\n");

        int ingested = documentIngestApi.ingestPaths(List.of(jsonl.toString()));
        assertEquals(2, ingested);

        List<DocumentSearchResult> clinicalOnly = documentSearchApi.searchChunksFaceted(
                "clinical treatment guidance",
                10,
                new DocumentSearchFilters("clinical", null, null, null));

        assertFalse(clinicalOnly.isEmpty());
        assertTrue(clinicalOnly.stream().allMatch(r -> "clinical".equals(r.category())));
    }

    @Test
    void shouldFilterSearchResultsBySourceInSql() throws Exception {
        Path jsonl = tempDir.resolve("source-faceted.jsonl");
        Files.writeString(jsonl,
                "{\"id\": \"s-1\", \"title\": \"WHO Doc\", \"category\": \"clinical\", \"source\": \"WHO\", \"text\": \"" + uniqueText("who-guidelines") + "\"}\n"
                        + "{\"id\": \"s-2\", \"title\": \"PubMed Doc\", \"category\": \"clinical\", \"source\": \"PubMed\", \"text\": \"" + uniqueText("pubmed-research") + "\"}\n");

        documentIngestApi.ingestPaths(List.of(jsonl.toString()));

        List<DocumentSearchResult> whoOnly = documentSearchApi.searchChunksFaceted(
                "treatment guidance",
                10,
                new DocumentSearchFilters(null, "WHO", null, null));

        assertFalse(whoOnly.isEmpty());
        assertTrue(whoOnly.stream().allMatch(r -> "WHO".equals(r.sourceName())));
    }
}
