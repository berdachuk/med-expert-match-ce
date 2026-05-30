package com.berdachuk.medexpertmatch.chat.repository.impl;

import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

@Component
public class ChatMessageMapper implements RowMapper<ChatMessage> {

    @Override
    public ChatMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ChatMessage(
                rs.getString("id"),
                rs.getString("chat_id"),
                rs.getString("role"),
                rs.getString("content"),
                rs.getInt("sequence_number"),
                rs.getObject("tokens_used", Integer.class),
                toInstant(rs.getTimestamp("created_at"))
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
