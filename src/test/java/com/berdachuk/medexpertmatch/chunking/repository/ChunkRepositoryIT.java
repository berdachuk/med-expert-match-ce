package com.berdachuk.medexpertmatch.chunking.repository;

import com.berdachuk.medexpertmatch.chunking.domain.DocumentChunk;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REQ-016: integration coverage for registered requirement.
 */
class ChunkRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private ChunkRepository chunkRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    private String documentId;

    @BeforeEach
    void setUp() {
        documentId = IdGenerator.generateId();
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.document_chunk");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.source_document");
        createSourceDocument(documentId);
    }

    private void createSourceDocument(String id) {
        String sql = "INSERT INTO medexpertmatch.source_document (id, content, content_hash, source_format) VALUES (:id, :content, :hash, :format)";
        Map<String, Object> params = Map.of("id", id, "content", "test", "hash", "hash-" + id, "format", "test");
        namedJdbcTemplate.update(sql, params);
    }

    @Test
    void shouldInsertAndFindByDocumentId() {
        DocumentChunk chunk = new DocumentChunk(IdGenerator.generateId(), documentId, 0, "Hello world", null);
        DocumentChunk inserted = chunkRepository.insert(chunk);
        assertNotNull(inserted);

        List<DocumentChunk> chunks = chunkRepository.findByDocumentId(documentId);
        assertEquals(1, chunks.size());
        assertEquals("Hello world", chunks.get(0).chunkText());
        assertEquals(0, chunks.get(0).chunkIndex());
    }

    @Test
    void shouldInsertBatch() {
        DocumentChunk chunk1 = new DocumentChunk(IdGenerator.generateId(), documentId, 0, "Chunk A", null);
        DocumentChunk chunk2 = new DocumentChunk(IdGenerator.generateId(), documentId, 1, "Chunk B", null);

        List<String> ids = chunkRepository.insertBatch(List.of(chunk1, chunk2));
        assertEquals(2, ids.size());

        List<DocumentChunk> chunks = chunkRepository.findByDocumentId(documentId);
        assertEquals(2, chunks.size());
    }

    @Test
    void shouldFindChunksOrderedByIndex() {
        DocumentChunk c0 = new DocumentChunk(IdGenerator.generateId(), documentId, 2, "Third", null);
        DocumentChunk c1 = new DocumentChunk(IdGenerator.generateId(), documentId, 0, "First", null);
        DocumentChunk c2 = new DocumentChunk(IdGenerator.generateId(), documentId, 1, "Second", null);

        chunkRepository.insertBatch(List.of(c0, c1, c2));

        List<DocumentChunk> chunks = chunkRepository.findByDocumentId(documentId);
        assertEquals(3, chunks.size());
        assertEquals(0, chunks.get(0).chunkIndex());
        assertEquals(1, chunks.get(1).chunkIndex());
        assertEquals(2, chunks.get(2).chunkIndex());
    }

    @Test
    void shouldDeleteByDocumentId() {
        DocumentChunk chunk = new DocumentChunk(IdGenerator.generateId(), documentId, 0, "Text", null);
        chunkRepository.insert(chunk);

        int deleted = chunkRepository.deleteByDocumentId(documentId);
        assertEquals(1, deleted);
        assertTrue(chunkRepository.findByDocumentId(documentId).isEmpty());
    }

    @Test
    void shouldDeleteAll() {
        String docId2 = IdGenerator.generateId();
        createSourceDocument(docId2);
        chunkRepository.insert(new DocumentChunk(IdGenerator.generateId(), documentId, 0, "A", null));
        chunkRepository.insert(new DocumentChunk(IdGenerator.generateId(), docId2, 0, "B", null));

        int deleted = chunkRepository.deleteAll();
        assertTrue(deleted >= 2);
        assertTrue(chunkRepository.findByDocumentId(documentId).isEmpty());
        assertTrue(chunkRepository.findByDocumentId(docId2).isEmpty());
    }

    @Test
    void shouldDeleteNonExistentDocument() {
        int deleted = chunkRepository.deleteByDocumentId("nonexistent123456");
        assertEquals(0, deleted);
    }

    @Test
    void shouldReturnEmptyForNonExistentDocument() {
        List<DocumentChunk> chunks = chunkRepository.findByDocumentId("nonexistent123456");
        assertTrue(chunks.isEmpty());
    }

    @Test
    void shouldInsertBatchWithEmptyList() {
        List<String> ids = chunkRepository.insertBatch(List.of());
        assertTrue(ids.isEmpty());
    }

    @Test
    void shouldInsertMultipleChunksForSameDocument() {
        for (int i = 0; i < 5; i++) {
            chunkRepository.insert(new DocumentChunk(IdGenerator.generateId(), documentId, i, "Chunk " + i, null));
        }

        List<DocumentChunk> chunks = chunkRepository.findByDocumentId(documentId);
        assertEquals(5, chunks.size());
    }

    @Test
    void shouldFindChunksWithNullEmbeddings() {
        chunkRepository.insert(new DocumentChunk(IdGenerator.generateId(), documentId, 0, "No embedding", null));
        chunkRepository.insert(new DocumentChunk(IdGenerator.generateId(), documentId, 1, "Also null", null));
        chunkRepository.insert(new DocumentChunk(IdGenerator.generateId(), documentId, 2, "Also no embedding", null));

        List<DocumentChunk> nullEmbeddingChunks = chunkRepository.findByEmbeddingIsNull(10);

        assertEquals(3, nullEmbeddingChunks.size());
        assertTrue(nullEmbeddingChunks.stream().allMatch(c -> c.embedding() == null));
    }

    @Test
    void shouldRespectLimitWhenFindingChunksWithNullEmbeddings() {
        chunkRepository.insert(new DocumentChunk(IdGenerator.generateId(), documentId, 0, "First", null));
        chunkRepository.insert(new DocumentChunk(IdGenerator.generateId(), documentId, 1, "Second", null));
        chunkRepository.insert(new DocumentChunk(IdGenerator.generateId(), documentId, 2, "Third", null));

        List<DocumentChunk> limited = chunkRepository.findByEmbeddingIsNull(2);

        assertEquals(2, limited.size());
    }

    @Test
    void shouldReturnEmptyWhenNoNullEmbeddings() {
        DocumentChunk chunk = new DocumentChunk(IdGenerator.generateId(), documentId, 0, "Embedded", null);
        chunkRepository.insert(chunk);
        float[] embedding768 = new float[768];
        embedding768[0] = 0.1f;
        chunkRepository.updateEmbedding(chunk.id(), embedding768);

        List<DocumentChunk> result = chunkRepository.findByEmbeddingIsNull(10);

        assertTrue(result.isEmpty());
    }
}
