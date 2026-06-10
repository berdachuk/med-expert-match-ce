package com.berdachuk.medexpertmatch.ingestion.syntheticdata.repository.impl;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain.SyntheticDataGenerationRun;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.repository.SyntheticDataGenerationRunRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class SyntheticDataGenerationRunRepositoryImpl implements SyntheticDataGenerationRunRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @InjectSql("/sql/syntheticdatarun/insert.sql")
    private String insertSql;

    @InjectSql("/sql/syntheticdatarun/update.sql")
    private String updateSql;

    @InjectSql("/sql/syntheticdatarun/findLatestBySize.sql")
    private String findLatestBySizeSql;

    @InjectSql("/sql/syntheticdatarun/findAll.sql")
    private String findAllSql;

    public SyntheticDataGenerationRunRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Override
    public String insert(SyntheticDataGenerationRun run) {
        Map<String, Object> params = toParams(run);
        namedJdbcTemplate.update(insertSql, params);
        return run.id();
    }

    @Override
    public void update(SyntheticDataGenerationRun run) {
        Map<String, Object> params = toParams(run);
        namedJdbcTemplate.update(updateSql, params);
    }

    @Override
    public List<SyntheticDataGenerationRun> findLatestBySize(String size, int limit) {
        Map<String, Object> params = Map.of("size", size, "limit", limit);
        return namedJdbcTemplate.query(findLatestBySizeSql, params, this::mapRow);
    }

    @Override
    public List<SyntheticDataGenerationRun> findAll() {
        return namedJdbcTemplate.query(findAllSql, Map.of(), this::mapRow);
    }

    private Map<String, Object> toParams(SyntheticDataGenerationRun run) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", run.id());
        params.put("size", run.size());
        params.put("doctorCount", run.doctorCount());
        params.put("caseCount", run.caseCount());
        params.put("startTime", run.startTime());
        params.put("endTime", run.endTime());
        params.put("totalDurationMs", run.totalDurationMs());
        params.put("descriptionMs", run.descriptionMs());
        params.put("embeddingMs", run.embeddingMs());
        params.put("clinicalExperienceMs", run.clinicalExperienceMs());
        params.put("graphBuildMs", run.graphBuildMs());
        params.put("errorMessage", run.errorMessage());
        return params;
    }

    private SyntheticDataGenerationRun mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SyntheticDataGenerationRun(
                rs.getString("id"),
                rs.getString("size"),
                rs.getInt("doctor_count"),
                rs.getInt("case_count"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time") != null ? rs.getTimestamp("end_time").toLocalDateTime() : null,
                rs.getObject("total_duration_ms") != null ? rs.getLong("total_duration_ms") : null,
                rs.getObject("description_ms") != null ? rs.getLong("description_ms") : null,
                rs.getObject("embedding_ms") != null ? rs.getLong("embedding_ms") : null,
                rs.getObject("clinical_experience_ms") != null ? rs.getLong("clinical_experience_ms") : null,
                rs.getObject("graph_build_ms") != null ? rs.getLong("graph_build_ms") : null,
                rs.getString("error_message")
        );
    }
}