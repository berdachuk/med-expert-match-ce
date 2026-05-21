package com.berdachuk.medexpertmatch.documents.service;

import com.berdachuk.medexpertmatch.chunking.domain.DocumentChunk;
import com.berdachuk.medexpertmatch.chunking.repository.ChunkRepository;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.documents.service.impl.DocumentEmbeddingPipeline;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocumentEmbeddingPipelineIT extends BaseIntegrationTest {

    @Autowired
    private DocumentEmbeddingPipeline documentEmbeddingPipeline;

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
    void shouldEmbedChunksAndPersistEmbeddings() {
        DocumentChunk chunk = new DocumentChunk(IdGenerator.generateId(), documentId, 0, "Hello world test chunk", null);
        chunkRepository.insert(chunk);

        documentEmbeddingPipeline.embedChunks(List.of(chunk));

        List<DocumentChunk> chunks = chunkRepository.findByDocumentId(documentId);
        assertFalse(chunks.isEmpty());
    }

    @Test
    void shouldNotFailOnEmptyList() {
        documentEmbeddingPipeline.embedChunks(List.of());
        documentEmbeddingPipeline.embedChunks(null);
    }

    @Test
    void shouldEmbedSingleChunk() {
        DocumentChunk chunk = new DocumentChunk(IdGenerator.generateId(), documentId, 0, "Single chunk for embedding test", null);
        chunkRepository.insert(chunk);

        documentEmbeddingPipeline.embedChunk(chunk);
    }
}
