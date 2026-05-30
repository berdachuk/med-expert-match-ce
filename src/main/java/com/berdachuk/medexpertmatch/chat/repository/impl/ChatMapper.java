package com.berdachuk.medexpertmatch.chat.repository.impl;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

@Component
public class ChatMapper implements RowMapper<Chat> {

    @Override
    public Chat mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Chat(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("name"),
                rs.getString("agent_id"),
                rs.getBoolean("is_default"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")),
                toInstant(rs.getTimestamp("last_activity_at")),
                rs.getInt("message_count")
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
