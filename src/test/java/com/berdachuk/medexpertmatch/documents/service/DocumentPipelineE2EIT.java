package com.berdachuk.medexpertmatch.documents.service;

import com.berdachuk.medexpertmatch.documents.DocumentIngestApi;
import com.berdachuk.medexpertmatch.documents.DocumentSearchApi;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REQ-016: integration coverage for registered requirement.
 */
class DocumentPipelineE2EIT extends BaseIntegrationTest {

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

    private static final String LONG_TEXT = "Hypertension is a chronic medical condition characterized by elevated blood "
            + "pressure in the arteries. It is a major risk factor for cardiovascular disease, stroke, and kidney failure. "
            + "Management of hypertension involves lifestyle modifications such as dietary changes, regular exercise, "
            + "weight management, and smoking cessation. Pharmacological treatment includes ACE inhibitors, angiotensin "
            + "receptor blockers (ARBs), calcium channel blockers, thiazide diuretics, and beta-blockers. First-line "
            + "therapy is typically initiated with ACE inhibitors or ARBs for most patients. Target blood pressure goals "
            + "are generally less than 130/80 mmHg for most adults with hypertension. Regular monitoring and medication "
            + "adherence are critical for achieving optimal blood pressure control and reducing cardiovascular risk. "
            + "Clinical guidelines from organizations such as the American Heart Association and the European Society "
            + "of Cardiology provide evidence-based recommendations for hypertension management in various patient populations.";

    @Test
    void shouldIngestChunkEmbedAndReturnRelevantSearchResults() throws Exception {
        Path jsonlFile = tempDir.resolve("pipeline-test.jsonl");
        Files.writeString(jsonlFile,
                "{\"id\": \"pipeline-1\", \"title\": \"Hypertension Management Guidelines 2024\", \"category\": \"clinical\", \"source\": \"PubMed\", \"url\": \"http://pubmed.example.com/htn\", \"text\": \"" + LONG_TEXT + "\"}\n");

        int ingestCount = documentIngestApi.ingestPaths(List.of(jsonlFile.toString()));
        assertEquals(1, ingestCount);

        List<Map<String, Object>> docs = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT id, title FROM medexpertmatch.source_document WHERE external_id = 'pipeline-1'");
        assertEquals(1, docs.size());
        assertEquals("Hypertension Management Guidelines 2024", docs.get(0).get("title"));
        String documentId = (String) docs.get(0).get("id");

        List<Map<String, Object>> chunks = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT id, chunk_text FROM medexpertmatch.document_chunk WHERE document_id = ?", documentId);
        assertFalse(chunks.isEmpty(), "Expected at least one chunk");
        for (Map<String, Object> chunk : chunks) {
            assertNotNull(chunk.get("id"));
            assertNotNull(chunk.get("chunk_text"));
            assertTrue(((String) chunk.get("chunk_text")).length() > 0);
        }

        List<DocumentSearchResult> results = documentSearchApi.searchChunks("hypertension management guidelines", 5);
        assertFalse(results.isEmpty(), "Search should return results for ingested content");
        assertTrue(results.stream().anyMatch(r -> r.documentTitle() != null
                && r.documentTitle().contains("Hypertension")), "Results should contain the ingested document");
        for (DocumentSearchResult result : results) {
            assertNotNull(result.chunkId());
            assertNotNull(result.chunkText());
            assertTrue(result.similarity() > 0.0, "Similarity score should be positive");
            assertTrue(result.similarity() <= 1.01, "Similarity should not exceed 1.0 (allow small float error)");
        }
    }

    @Test
    void shouldReturnResultsAfterIngest() throws Exception {
        Path jsonlFile = tempDir.resolve("ingest-then-search.jsonl");
        Files.writeString(jsonlFile,
                "{\"id\": \"ingest-1\", \"title\": \"Oncology Treatment Review\", \"category\": \"clinical\", \"source\": \"PubMed\", \"text\": \"This document covers oncology treatment protocols for various cancer types including chemotherapy regimens and radiation therapy approaches. Clinical outcomes for breast cancer, lung cancer, and colorectal cancer are reviewed with evidence-based recommendations for each cancer type.\"}\n");

        documentIngestApi.ingestPaths(List.of(jsonlFile.toString()));

        List<DocumentSearchResult> results = documentSearchApi.searchChunks("cancer treatment oncology", 5);
        assertFalse(results.isEmpty(), "Ingested documents should be searchable");
    }

    @Test
    void shouldHandleEmptyDocumentCorpusSearch() {
        List<DocumentSearchResult> results = documentSearchApi.searchChunks("any medical query", 5);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
