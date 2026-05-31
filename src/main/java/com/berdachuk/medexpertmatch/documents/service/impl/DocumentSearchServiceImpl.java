package com.berdachuk.medexpertmatch.documents.service.impl;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.documents.DocumentSearchApi;
import com.berdachuk.medexpertmatch.documents.domain.DocumentSearchFilters;
import com.berdachuk.medexpertmatch.documents.domain.DocumentSearchResult;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "medexpertmatch.documents.enabled", havingValue = "true")
public class DocumentSearchServiceImpl implements DocumentSearchApi {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final EmbeddingService embeddingService;

    @InjectSql("/sql/document/searchChunksFaceted.sql")
    private String searchChunksFacetedSql;

    public DocumentSearchServiceImpl(NamedParameterJdbcTemplate namedJdbcTemplate,
                                     EmbeddingService embeddingService) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.embeddingService = embeddingService;
    }

    @Override
    public List<DocumentSearchResult> searchChunks(String query, int topK) {
        return searchChunksFaceted(query, topK, DocumentSearchFilters.none());
    }

    @Override
    public List<DocumentSearchResult> searchChunksFaceted(String query, int topK, DocumentSearchFilters filters) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        DocumentSearchFilters effectiveFilters = filters != null ? filters : DocumentSearchFilters.none();

        float[] queryEmbedding = embeddingService.generateEmbeddingAsFloatArray(query);
        String vectorString = formatVector(queryEmbedding);

        Map<String, Object> params = new HashMap<>();
        params.put("queryEmbedding", vectorString);
        params.put("limit", topK > 0 ? topK : 10);
        params.put("category", blankToNull(effectiveFilters.category()));
        params.put("source", blankToNull(effectiveFilters.source()));
        params.put("fromDate", effectiveFilters.fromDate() != null
                ? effectiveFilters.fromDate().atStartOfDay()
                : null);
        params.put("toDate", effectiveFilters.toDate() != null
                ? effectiveFilters.toDate().atTime(23, 59, 59)
                : null);

        return namedJdbcTemplate.query(searchChunksFacetedSql, params, (rs, rowNum) -> new DocumentSearchResult(
                rs.getString("id"),
                rs.getString("document_id"),
                rs.getInt("chunk_index"),
                rs.getString("chunk_text"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("source_name"),
                rs.getDouble("similarity")
        ));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%.6f", vector[i]));
        }
        sb.append("]");
        return sb.toString();
    }
}
