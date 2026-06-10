package com.berdachuk.medexpertmatch.documents.service;

import com.berdachuk.medexpertmatch.documents.DocumentSearchApi;
import com.berdachuk.medexpertmatch.documents.domain.DocumentSearchResult;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentSearchServiceTest {

    private DocumentSearchApi createService(NamedParameterJdbcTemplate jdbcTemplate, EmbeddingService embeddingService) throws Exception {
        var constructor = Class.forName("com.berdachuk.medexpertmatch.documents.service.impl.DocumentSearchServiceImpl")
                .getDeclaredConstructor(NamedParameterJdbcTemplate.class, EmbeddingService.class);
        constructor.setAccessible(true);
        var service = constructor.newInstance(jdbcTemplate, embeddingService);
        ReflectionTestUtils.setField(service, "searchChunksFacetedSql",
                "SELECT dc.id, dc.document_id, dc.chunk_index, dc.chunk_text, " +
                "sd.title, sd.category, sd.source_name, sd.created_at, " +
                "1 - (dc.embedding <=> :queryEmbedding::vector) AS similarity " +
                "FROM medexpertmatch.document_chunk dc " +
                "JOIN medexpertmatch.source_document sd ON dc.document_id = sd.id " +
                "WHERE dc.embedding IS NOT NULL LIMIT :limit");
        return (DocumentSearchApi) service;
    }

    @Test
    @DisplayName("searchChunks returns empty list for blank query")
    void emptyQueryReturnsEmptyList() throws Exception {
        var jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        var embeddingService = mock(EmbeddingService.class);
        DocumentSearchApi service = createService(jdbcTemplate, embeddingService);

        List<DocumentSearchResult> results = service.searchChunks("", 10);

        assertTrue(results.isEmpty());
        verifyNoInteractions(embeddingService);
    }

    @Test
    @DisplayName("searchChunks returns empty list for null query")
    void nullQueryReturnsEmptyList() throws Exception {
        var jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        var embeddingService = mock(EmbeddingService.class);
        DocumentSearchApi service = createService(jdbcTemplate, embeddingService);

        List<DocumentSearchResult> results = service.searchChunks(null, 10);

        assertTrue(results.isEmpty());
        verifyNoInteractions(embeddingService);
    }

    @Test
    @DisplayName("searchChunksFaceted handles null filters gracefully")
    void nullFiltersDoesNotThrow() throws Exception {
        var jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        var embeddingService = mock(EmbeddingService.class);
        DocumentSearchApi service = createService(jdbcTemplate, embeddingService);

        when(embeddingService.generateEmbeddingAsFloatArray(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(jdbcTemplate.query(anyString(), anyMap(), any(RowMapper.class)))
                .thenReturn(List.of());

        List<DocumentSearchResult> results = service.searchChunksFaceted("test", 5, null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("searchChunks returns results for valid query")
    void validQueryReturnsResults() throws Exception {
        var jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        var embeddingService = mock(EmbeddingService.class);
        DocumentSearchApi service = createService(jdbcTemplate, embeddingService);

        when(embeddingService.generateEmbeddingAsFloatArray(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(jdbcTemplate.query(anyString(), any(Map.class), any(RowMapper.class)))
                .thenReturn(List.of(new DocumentSearchResult(
                        "chunk-1", "doc-1", 0, "heart treatment", "Cardio Guidelines",
                        "cardiology", "NEJM", 0.95)));

        List<DocumentSearchResult> results = service.searchChunks("heart disease", 5);

        assertEquals(1, results.size());
        assertEquals("heart treatment", results.getFirst().chunkText());
        assertEquals(0.95, results.getFirst().similarity());
    }
}