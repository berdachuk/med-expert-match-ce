package com.berdachuk.medexpertmatch.retrieval.repository.impl;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.retrieval.domain.ConsultationMatch;
import com.berdachuk.medexpertmatch.retrieval.repository.ConsultationMatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Repository for consultation match persistence.
 */
@Slf4j
@Repository
public class ConsultationMatchRepositoryImpl implements ConsultationMatchRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @InjectSql("/sql/consultationmatch/insert.sql")
    private String insertSql;

    @InjectSql("/sql/consultationmatch/deleteByCaseId.sql")
    private String deleteByCaseIdSql;

    @InjectSql("/sql/consultationmatch/count.sql")
    private String countSql;

    @InjectSql("/sql/consultationmatch/deleteAll.sql")
    private String deleteAllSql;

    public ConsultationMatchRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Override
    public void deleteByCaseId(String caseId) {
        if (caseId == null || caseId.isBlank()) {
            return;
        }
        Map<String, Object> params = Map.of("caseId", caseId.trim().toLowerCase());
        namedJdbcTemplate.update(deleteByCaseIdSql, params);
    }

    @Override
    public List<String> insertBatch(List<ConsultationMatch> matches) {
        if (matches.isEmpty()) {
            return List.of();
        }
        SqlParameterSource[] batch = matches.stream()
                .map(m -> new MapSqlParameterSource()
                        .addValue("id", m.id())
                        .addValue("caseId", m.caseId())
                        .addValue("doctorId", m.doctorId())
                        .addValue("matchScore", m.matchScore())
                        .addValue("matchRationale", m.matchRationale())
                        .addValue("rank", m.rank())
                        .addValue("status", m.status()))
                .toArray(SqlParameterSource[]::new);
        namedJdbcTemplate.batchUpdate(insertSql, batch);
        return matches.stream().map(ConsultationMatch::id).toList();
    }

    @Override
    public long count() {
        Long result = namedJdbcTemplate.queryForObject(countSql, Map.of(), Long.class);
        return result != null ? result : 0L;
    }

    @Override
    public int deleteAll() {
        return namedJdbcTemplate.getJdbcTemplate().update(deleteAllSql);
    }
}
