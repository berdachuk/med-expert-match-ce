package com.berdachuk.medexpertmatch.core.repository.impl;

import com.berdachuk.medexpertmatch.core.domain.ApiSessionToken;
import com.berdachuk.medexpertmatch.core.repository.ApiSessionTokenRepository;
import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class ApiSessionTokenRepositoryImpl implements ApiSessionTokenRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ApiSessionTokenMapper apiSessionTokenMapper;

    @InjectSql("/sql/core/apiSessionTokenInsert.sql")
    private String insertSql;

    @InjectSql("/sql/core/apiSessionTokenFindByApiKey.sql")
    private String findByApiKeySql;

    @InjectSql("/sql/core/apiSessionTokenFindAll.sql")
    private String findAllSql;

    @InjectSql("/sql/core/apiSessionTokenDeleteById.sql")
    private String deleteByIdSql;

    @InjectSql("/sql/core/apiSessionTokenUpdateLastUsed.sql")
    private String updateLastUsedSql;

    public ApiSessionTokenRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            ApiSessionTokenMapper apiSessionTokenMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.apiSessionTokenMapper = apiSessionTokenMapper;
    }

    @Override
    public String insert(ApiSessionToken token) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", token.id())
                .addValue("apiKey", token.apiKey())
                .addValue("description", token.description())
                .addValue("rateLimitTier", token.rateLimitTier().toDatabaseValue())
                .addValue("expiresAt", toTimestamp(token.expiresAt()))
                .addValue("createdAt", toTimestamp(token.createdAt()))
                .addValue("lastUsedAt", toTimestamp(token.lastUsedAt()));
        namedJdbcTemplate.update(insertSql, params);
        return token.id();
    }

    @Override
    public Optional<ApiSessionToken> findByApiKey(String apiKey) {
        List<ApiSessionToken> results = namedJdbcTemplate.query(
                findByApiKeySql,
                new MapSqlParameterSource("apiKey", apiKey),
                apiSessionTokenMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public List<ApiSessionToken> findAll() {
        return namedJdbcTemplate.query(findAllSql, apiSessionTokenMapper);
    }

    @Override
    public boolean deleteById(String id) {
        return namedJdbcTemplate.update(deleteByIdSql, new MapSqlParameterSource("id", id)) > 0;
    }

    @Override
    public void updateLastUsedAt(String id, Instant lastUsedAt) {
        namedJdbcTemplate.update(
                updateLastUsedSql,
                new MapSqlParameterSource("id", id).addValue("lastUsedAt", toTimestamp(lastUsedAt)));
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }
}
