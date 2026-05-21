package com.berdachuk.medexpertmatch.documents.repository.impl;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.documents.domain.SourceDocumentEntity;
import com.berdachuk.medexpertmatch.documents.repository.SourceDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class SourceDocumentRepositoryImpl implements SourceDocumentRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final SourceDocumentMapper sourceDocumentMapper;

    @InjectSql("/sql/document/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/document/findByContentHash.sql")
    private String findByContentHashSql;

    @InjectSql("/sql/document/findByExternalId.sql")
    private String findByExternalIdSql;

    @InjectSql("/sql/document/findAll.sql")
    private String findAllSql;

    @InjectSql("/sql/document/findAllIds.sql")
    private String findAllIdsSql;

    @InjectSql("/sql/document/insert.sql")
    private String insertSql;

    @InjectSql("/sql/document/insertBatch.sql")
    private String insertBatchSql;

    @InjectSql("/sql/document/deleteAll.sql")
    private String deleteAllSql;

    @InjectSql("/sql/document/findCategories.sql")
    private String findCategoriesSql;

    public SourceDocumentRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            SourceDocumentMapper sourceDocumentMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.sourceDocumentMapper = sourceDocumentMapper;
    }

    @Override
    public Optional<SourceDocumentEntity> findById(String id) {
        Map<String, Object> params = Map.of("id", id);
        List<SourceDocumentEntity> results = namedJdbcTemplate.query(findByIdSql, params, sourceDocumentMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public Optional<SourceDocumentEntity> findByContentHash(String contentHash) {
        Map<String, Object> params = Map.of("contentHash", contentHash);
        List<SourceDocumentEntity> results = namedJdbcTemplate.query(findByContentHashSql, params, sourceDocumentMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public Optional<SourceDocumentEntity> findByExternalId(String externalId) {
        Map<String, Object> params = Map.of("externalId", externalId);
        List<SourceDocumentEntity> results = namedJdbcTemplate.query(findByExternalIdSql, params, sourceDocumentMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public List<SourceDocumentEntity> findAll(int limit) {
        Map<String, Object> params = Map.of("limit", limit > 0 ? limit : Integer.MAX_VALUE);
        return namedJdbcTemplate.query(findAllSql, params, sourceDocumentMapper);
    }

    @Override
    public List<String> findAllIds(int limit) {
        Map<String, Object> params = Map.of("limit", limit > 0 ? limit : Integer.MAX_VALUE);
        return namedJdbcTemplate.query(findAllIdsSql, params, (rs, rowNum) -> rs.getString("id"));
    }

    @Override
    public String insert(SourceDocumentEntity document) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", document.id());
        params.put("externalId", document.externalId());
        params.put("title", document.title());
        params.put("category", document.category());
        params.put("sourceName", document.sourceName());
        params.put("sourceUrl", document.sourceUrl());
        params.put("content", document.content());
        params.put("contentHash", document.contentHash());
        params.put("sourceFormat", document.sourceFormat());
        return namedJdbcTemplate.queryForObject(insertSql, params, String.class);
    }

    @Override
    public List<String> insertBatch(List<SourceDocumentEntity> documents) {
        if (documents.isEmpty()) {
            return List.of();
        }

        SqlParameterSource[] batchParams = documents.stream()
                .map(doc -> new MapSqlParameterSource()
                        .addValue("id", doc.id())
                        .addValue("externalId", doc.externalId())
                        .addValue("title", doc.title())
                        .addValue("category", doc.category())
                        .addValue("sourceName", doc.sourceName())
                        .addValue("sourceUrl", doc.sourceUrl())
                        .addValue("content", doc.content())
                        .addValue("contentHash", doc.contentHash())
                        .addValue("sourceFormat", doc.sourceFormat()))
                .toArray(SqlParameterSource[]::new);

        namedJdbcTemplate.batchUpdate(insertBatchSql, batchParams);
        return documents.stream().map(SourceDocumentEntity::id).collect(Collectors.toList());
    }

    @Override
    public int deleteAll() {
        return namedJdbcTemplate.update(deleteAllSql, Map.of());
    }

    @Override
    public List<String> findCategories() {
        return namedJdbcTemplate.query(findCategoriesSql, Map.of(), (rs, rowNum) -> rs.getString("category"));
    }
}
