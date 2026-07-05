package com.berdachuk.medexpertmatch.documents.service;

import com.berdachuk.medexpertmatch.documents.DocumentIngestApi;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
class DocumentIngestServiceIT extends BaseIntegrationTest {

    @Autowired
    private DocumentIngestApi documentIngestApi;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.document_chunk");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.source_document");
    }

    private static final String LONG_TEXT_TEMPLATE = "This is a detailed medical document that contains substantial content for chunking purposes. "
            + "It covers multiple paragraphs and topics related to clinical practice guidelines and evidence-based medicine. "
            + "The content includes detailed descriptions of treatment protocols, diagnostic criteria, and patient management strategies. "
            + "This document serves as an important reference for healthcare professionals seeking to understand %s. "
            + "Multiple studies have shown the effectiveness of these approaches in improving patient outcomes and reducing complications. "
            + "Further research is needed to validate these findings in diverse patient populations and clinical settings.";

    private String longText(String topic) {
        return String.format(LONG_TEXT_TEMPLATE, topic);
    }

    @Test
    void shouldIngestJsonlFileAndPersistDocuments() throws Exception {
        Path jsonlFile = tempDir.resolve("test-docs.jsonl");
        Files.writeString(jsonlFile,
                "{\"id\": \"ext-1\", \"title\": \"Doc One\", \"category\": \"clinical\", \"source\": \"Test\", \"url\": \"http://example.com/1\", \"text\": \"" + longText("hypertension management") + "\"}\n"
                        + "{\"id\": \"ext-2\", \"title\": \"Doc Two\", \"category\": \"research\", \"source\": \"Test\", \"url\": \"http://example.com/2\", \"text\": \"" + longText("diabetes care protocols") + "\"}\n"
                        + "{\"id\": \"ext-3\", \"title\": \"Doc Three\", \"category\": \"clinical\", \"source\": \"Test\", \"url\": \"http://example.com/3\", \"text\": \"" + longText("cardiovascular disease treatment") + "\"}\n");

        int count = documentIngestApi.ingestPaths(List.of(jsonlFile.toString()));

        assertEquals(3, count);
        List<Map<String, Object>> docs = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.source_document ORDER BY title");
        assertEquals(3, docs.size());
        assertEquals("Doc One", docs.get(0).get("title"));
        assertEquals("Doc Three", docs.get(1).get("title"));
        assertEquals("Doc Two", docs.get(2).get("title"));

        List<Map<String, Object>> chunks = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.document_chunk");
        assertTrue(chunks.size() > 0, "Expected at least one chunk across all documents");
    }

    @Test
    void shouldCreateChunksForIngestedDocuments() throws Exception {
        Path jsonlFile = tempDir.resolve("chunk-test.jsonl");
        Files.writeString(jsonlFile,
                "{\"id\": \"chunk-ext-1\", \"title\": \"Chunk Test\", \"category\": \"research\", \"source\": \"Test\", \"url\": \"http://example.com/chunk\", \"text\": \"" + longText("neurological disorder management and treatment guidelines") + "\"}\n");

        documentIngestApi.ingestPaths(List.of(jsonlFile.toString()));

        List<Map<String, Object>> docs = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT id, external_id, category, source_format FROM medexpertmatch.source_document WHERE external_id = 'chunk-ext-1'");
        assertEquals(1, docs.size());
        assertEquals("jsonl", docs.get(0).get("source_format"));

        List<Map<String, Object>> chunks = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.document_chunk WHERE document_id = ?", docs.get(0).get("id"));

        assertFalse(chunks.isEmpty(), "Expected at least one chunk");
        for (Map<String, Object> chunk : chunks) {
            assertNotNull(chunk.get("id"));
            assertEquals(docs.get(0).get("id"), chunk.get("document_id"));
            assertNotNull(chunk.get("chunk_text"));
            assertTrue(((String) chunk.get("chunk_text")).length() > 0);
            assertTrue(chunk.containsKey("embedding"), "Chunk should have embedding column");
        }
    }

    @Test
    void shouldDeduplicateByContentHash() throws Exception {
        Path jsonlFile = tempDir.resolve("dedup-test.jsonl");
        String text = longText("oncology treatment protocols and chemotherapy guidelines");
        Files.writeString(jsonlFile,
                "{\"id\": \"dedup-1\", \"title\": \"Unique Doc\", \"category\": \"clinical\", \"source\": \"Test\", \"text\": \"" + text + "\"}\n");

        int firstCount = documentIngestApi.ingestPaths(List.of(jsonlFile.toString()));
        assertEquals(1, firstCount);

        int secondCount = documentIngestApi.ingestPaths(List.of(jsonlFile.toString()));

        List<Map<String, Object>> docs = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.source_document");
        assertEquals(1, docs.size(), "Only one document should exist after deduplication");

        List<Map<String, Object>> chunks = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.document_chunk");
        int firstChunkCount = chunks.size();

        int thirdCount = documentIngestApi.ingestPaths(List.of(jsonlFile.toString()));

        List<Map<String, Object>> docsAfterThird = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.source_document");
        assertEquals(1, docsAfterThird.size(), "Still only one document after third ingest");

        List<Map<String, Object>> chunksAfterThird = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.document_chunk");
        assertEquals(firstChunkCount, chunksAfterThird.size(), "Chunk count should not change after re-ingest");
    }

    @Test
    void shouldSkipUnsupportedFileFormat() throws Exception {
        Path unsupportedFile = tempDir.resolve("test.txt");
        Files.writeString(unsupportedFile, "This is a plain text file with unsupported format.");

        int count = documentIngestApi.ingestPaths(List.of(unsupportedFile.toString()));

        assertEquals(0, count);
        List<Map<String, Object>> docs = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.source_document");
        assertTrue(docs.isEmpty(), "Unsupported format should produce no documents");
    }

    @Test
    void shouldSkipNonExistentFileGracefully() {
        Path nonExistent = tempDir.resolve("does-not-exist.jsonl");

        int count = documentIngestApi.ingestPaths(List.of(nonExistent.toString()));

        assertEquals(0, count);
    }

    @Test
    void shouldIngestFromDirectory() throws Exception {
        Path docDir = tempDir.resolve("docs");
        Files.createDirectory(docDir);

        Files.writeString(docDir.resolve("a.jsonl"),
                "{\"id\": \"dir-1\", \"title\": \"Directory Doc 1\", \"category\": \"clinical\", \"source\": \"Test\", \"text\": \"" + longText("cardiovascular disease management") + "\"}\n");
        Files.writeString(docDir.resolve("b.jsonl"),
                "{\"id\": \"dir-2\", \"title\": \"Directory Doc 2\", \"category\": \"research\", \"source\": \"Test\", \"text\": \"" + longText("neurological disorders and treatment") + "\"}\n");

        int count = documentIngestApi.ingestFromDirectory(docDir.toString());

        assertEquals(2, count);
        List<Map<String, Object>> docs = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.source_document ORDER BY title");
        assertEquals(2, docs.size());
    }

    @Test
    void shouldHandleMixedValidAndInvalidPaths() throws Exception {
        Path validFile = tempDir.resolve("mixed-valid.jsonl");
        Files.writeString(validFile,
                "{\"id\": \"mixed-1\", \"title\": \"Mixed Valid\", \"category\": \"clinical\", \"source\": \"Test\", \"text\": \"" + longText("mixed-path ingestion test validation") + "\"}\n");

        int count = documentIngestApi.ingestPaths(List.of(
                validFile.toString(),
                tempDir.resolve("nonexistent.jsonl").toString(),
                tempDir.resolve("bad.txt").toString()
        ));

        assertEquals(1, count);
        List<Map<String, Object>> docs = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.source_document");
        assertEquals(1, docs.size());
    }

    private Path createTestPdf(Path dir, String name, String content) throws Exception {
        Path pdfFile = dir.resolve(name);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.beginText();
                stream.newLineAtOffset(25, 700);
                String[] words = content.split(" ");
                StringBuilder line = new StringBuilder();
                for (String word : words) {
                    if (line.length() + word.length() > 80) {
                        stream.showText(line.toString().trim());
                        stream.newLineAtOffset(0, -15);
                        line = new StringBuilder();
                    }
                    line.append(word).append(" ");
                }
                if (!line.isEmpty()) {
                    stream.showText(line.toString().trim());
                }
                stream.endText();
            }
            document.save(pdfFile.toFile());
        }
        return pdfFile;
    }

    @Test
    void shouldIngestPdfFileAndPersistDocument() throws Exception {
        String pdfContent = "Clinical Guidelines for Hypertension Management. "
                + "These evidence-based guidelines cover pharmacological and lifestyle interventions "
                + "for managing hypertension. First-line therapy includes ACE inhibitors, ARBs, "
                + "calcium channel blockers, and thiazide diuretics. Treatment targets should be "
                + "individualized based on patient age, comorbidities, and cardiovascular risk. "
                + "Lifestyle modifications include sodium reduction, regular exercise, weight "
                + "management, and limiting alcohol intake. Regular monitoring and follow-up are "
                + "essential for optimizing blood pressure control and minimizing complications.";
        Path pdfFile = createTestPdf(tempDir, "hypertension-guidelines.pdf", pdfContent);

        int count = documentIngestApi.ingestPaths(List.of(pdfFile.toString()));

        assertEquals(1, count);
        List<Map<String, Object>> docs = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.source_document WHERE source_format = 'pdf'");
        assertEquals(1, docs.size());
        assertEquals("hypertension-guidelines", docs.get(0).get("title"));
        assertNotNull(docs.get(0).get("content_hash"));

        List<Map<String, Object>> chunks = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.document_chunk WHERE document_id = ?",
                        docs.get(0).get("id"));
        assertFalse(chunks.isEmpty(), "Expected at least one chunk for PDF document");
    }

    @Test
    void shouldSkipCorruptedPdf() throws Exception {
        Path corruptedPdf = tempDir.resolve("corrupted.pdf");
        Files.writeString(corruptedPdf, "not-a-valid-pdf-binary-content");

        int count = documentIngestApi.ingestPaths(List.of(corruptedPdf.toString()));

        assertEquals(0, count);
        List<Map<String, Object>> docs = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.source_document");
        assertTrue(docs.isEmpty(), "Corrupted PDF should not persist any document");
    }

    @Test
    void shouldDeduplicatePdfByContentHash() throws Exception {
        String pdfContent = "Clinical Guidelines for Diabetes Management. "
                + "These guidelines cover type 2 diabetes pharmacological management "
                + "with metformin as first-line therapy, followed by SGLT2 inhibitors "
                + "or GLP-1 receptor agonists as add-on therapy. Regular A1C monitoring "
                + "is recommended every 3-6 months with target A1C below 7.0 percent.";
        Path pdfFile = createTestPdf(tempDir, "diabetes-guidelines.pdf", pdfContent);

        int firstCount = documentIngestApi.ingestPaths(List.of(pdfFile.toString()));
        assertEquals(1, firstCount);

        int secondCount = documentIngestApi.ingestPaths(List.of(pdfFile.toString()));
        assertEquals(0, secondCount, "Deduplication should skip second ingest");

        List<Map<String, Object>> docs = namedJdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT * FROM medexpertmatch.source_document WHERE source_format = 'pdf'");
        assertEquals(1, docs.size(), "Only one PDF document should exist");
    }
}
