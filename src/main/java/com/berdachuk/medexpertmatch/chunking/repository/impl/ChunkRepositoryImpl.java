package com.berdachuk.medexpertmatch.chunking.repository.impl;

import com.berdachuk.medexpertmatch.chunking.domain.DocumentChunk;
import com.berdachuk.medexpertmatch.chunking.repository.ChunkRepository;
import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class ChunkRepositoryImpl implements ChunkRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ChunkMapper chunkMapper;

    @InjectSql("/sql/chunk/findByDocumentId.sql")
    private String findByDocumentIdSql;

    @InjectSql("/sql/chunk/insert.sql")
    private String insertSql;

    @InjectSql("/sql/chunk/insertBatch.sql")
    private String insertBatchSql;

    @InjectSql("/sql/chunk/deleteByDocumentId.sql")
    private String deleteByDocumentIdSql;

    @InjectSql("/sql/chunk/deleteAll.sql")
    private String deleteAllSql;

    @InjectSql("/sql/chunk/updateEmbedding.sql")
    private String updateEmbeddingSql;

    @InjectSql("/sql/chunk/findByEmbeddingIsNull.sql")
    private String findByEmbeddingIsNullSql;

    public ChunkRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            ChunkMapper chunkMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.chunkMapper = chunkMapper;
    }

    @Override
    public List<DocumentChunk> findByDocumentId(String documentId) {
        Map<String, Object> params = Map.of("documentId", documentId);
        return namedJdbcTemplate.query(findByDocumentIdSql, params, chunkMapper);
    }

    @Override
    public DocumentChunk insert(DocumentChunk chunk) {
        Map<String, Object> params = Map.of(
                "id", chunk.id(),
                "documentId", chunk.documentId(),
                "chunkIndex", chunk.chunkIndex(),
                "chunkText", chunk.chunkText()
        );
        namedJdbcTemplate.queryForObject(insertSql, params, String.class);
        return chunk;
    }

    @Override
    public List<String> insertBatch(List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return List.of();
        }

        SqlParameterSource[] batchParams = chunks.stream()
                .map(chunk -> new MapSqlParameterSource()
                        .addValue("id", chunk.id())
                        .addValue("documentId", chunk.documentId())
                        .addValue("chunkIndex", chunk.chunkIndex())
                        .addValue("chunkText", chunk.chunkText()))
                .toArray(SqlParameterSource[]::new);

        namedJdbcTemplate.batchUpdate(insertBatchSql, batchParams);
        return chunks.stream().map(DocumentChunk::id).collect(Collectors.toList());
    }

    @Override
    public int deleteByDocumentId(String documentId) {
        Map<String, Object> params = Map.of("documentId", documentId);
        return namedJdbcTemplate.update(deleteByDocumentIdSql, params);
    }

    @Override
    public int deleteAll() {
        return namedJdbcTemplate.update(deleteAllSql, Map.of());
    }

    @Override
    public List<DocumentChunk> findByEmbeddingIsNull(int limit) {
        return namedJdbcTemplate.query(findByEmbeddingIsNullSql, Map.of("limit", limit), chunkMapper);
    }

    public void updateEmbedding(String chunkId, float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return;
        }
        String vectorString = formatVector(embedding);
        Map<String, Object> params = Map.of("id", chunkId, "embedding", vectorString);
        namedJdbcTemplate.update(updateEmbeddingSql, params);
    }

    public void updateEmbeddings(List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return;
        }

        SqlParameterSource[] batchParams = chunks.stream()
                .filter(chunk -> chunk.embedding() != null && chunk.embedding().length > 0)
                .map(chunk -> new MapSqlParameterSource()
                        .addValue("id", chunk.id())
                        .addValue("embedding", formatVector(chunk.embedding())))
                .toArray(SqlParameterSource[]::new);

        if (batchParams.length > 0) {
            namedJdbcTemplate.batchUpdate(updateEmbeddingSql, batchParams);
        }
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
