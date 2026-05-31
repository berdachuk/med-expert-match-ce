package com.berdachuk.medexpertmatch.core.repository.impl;

import com.berdachuk.medexpertmatch.core.domain.ApiSessionToken;
import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

@Component
public class ApiSessionTokenMapper implements RowMapper<ApiSessionToken> {

    @Override
    public ApiSessionToken mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ApiSessionToken(
                rs.getString("id"),
                rs.getString("api_key"),
                rs.getString("description"),
                RateLimitTier.fromDatabaseValue(rs.getString("rate_limit_tier")),
                toInstant(rs.getTimestamp("expires_at")),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("last_used_at"))
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
