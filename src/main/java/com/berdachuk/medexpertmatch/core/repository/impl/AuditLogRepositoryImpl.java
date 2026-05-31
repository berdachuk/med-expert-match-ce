package com.berdachuk.medexpertmatch.core.repository.impl;

import com.berdachuk.medexpertmatch.core.domain.AuditLog;
import com.berdachuk.medexpertmatch.core.repository.AuditLogRepository;
import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @InjectSql("/sql/core/auditLogInsert.sql")
    private String insertSql;

    @InjectSql("/sql/core/auditLogFindByAction.sql")
    private String findByActionSql;

    @InjectSql("/sql/core/auditLogFindByActionPaged.sql")
    private String findByActionPagedSql;

    @InjectSql("/sql/core/auditLogFindByActionsPaged.sql")
    private String findByActionsPagedSql;

    public AuditLogRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public String insert(AuditLog auditLog) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", auditLog.id())
                .addValue("action", auditLog.action())
                .addValue("resourceType", auditLog.resourceType())
                .addValue("resourceId", auditLog.resourceId())
                .addValue("actor", auditLog.actor())
                .addValue("details", serializeDetails(auditLog.details()))
                .addValue("createdAt", toTimestamp(auditLog.createdAt()));
        namedJdbcTemplate.update(insertSql, params);
        return auditLog.id();
    }

    @Override
    public List<AuditLog> findByAction(String action, int limit) {
        return namedJdbcTemplate.query(
                findByActionSql,
                new MapSqlParameterSource("action", action).addValue("limit", limit),
                auditLogMapper);
    }

    @Override
    public List<AuditLog> findByAction(String action, int limit, int offset) {
        return namedJdbcTemplate.query(
                findByActionPagedSql,
                new MapSqlParameterSource("action", action)
                        .addValue("limit", limit)
                        .addValue("offset", offset),
                auditLogMapper);
    }

    @Override
    public List<AuditLog> findByActions(List<String> actions, int limit, int offset) {
        return namedJdbcTemplate.query(
                findByActionsPagedSql,
                new MapSqlParameterSource("actions", actions)
                        .addValue("limit", limit)
                        .addValue("offset", offset),
                auditLogMapper);
    }

    private String serializeDetails(Map<String, Object> details) {
        Map<String, Object> safeDetails = details != null ? details : Map.of();
        try {
            return objectMapper.writeValueAsString(safeDetails);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit log details", e);
            return "{}";
        }
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }
}
