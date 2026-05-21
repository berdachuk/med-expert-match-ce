package com.berdachuk.medexpertmatch.documents.service.impl;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.documents.DocumentSearchApi;
import com.berdachuk.medexpertmatch.documents.domain.DocumentSearchResult;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "medexpertmatch.documents.enabled", havingValue = "true")
public class DocumentSearchServiceImpl implements DocumentSearchApi {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final EmbeddingService embeddingService;

    @InjectSql("/sql/document/searchChunks.sql")
    private String searchChunksSql;

    public DocumentSearchServiceImpl(NamedParameterJdbcTemplate namedJdbcTemplate,
                                     EmbeddingService embeddingService) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.embeddingService = embeddingService;
    }

    @Override
    public List<DocumentSearchResult> searchChunks(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        float[] queryEmbedding = embeddingService.generateEmbeddingAsFloatArray(query);
        String vectorString = formatVector(queryEmbedding);

        Map<String, Object> params = Map.of(
                "queryEmbedding", vectorString,
                "limit", topK > 0 ? topK : 10
        );

        return namedJdbcTemplate.query(searchChunksSql, params, (rs, rowNum) -> new DocumentSearchResult(
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
