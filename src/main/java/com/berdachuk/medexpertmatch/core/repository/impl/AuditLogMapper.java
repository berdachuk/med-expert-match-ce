package com.berdachuk.medexpertmatch.core.repository.impl;

import com.berdachuk.medexpertmatch.core.domain.AuditLog;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Component
public class AuditLogMapper implements RowMapper<AuditLog> {

    private static final TypeReference<Map<String, Object>> DETAILS_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public AuditLogMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AuditLog mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AuditLog(
                rs.getString("id"),
                rs.getString("action"),
                rs.getString("resource_type"),
                rs.getString("resource_id"),
                rs.getString("actor"),
                parseDetails(rs.getString("details")),
                toInstant(rs.getTimestamp("created_at"))
        );
    }

    private Map<String, Object> parseDetails(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, DETAILS_TYPE);
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
