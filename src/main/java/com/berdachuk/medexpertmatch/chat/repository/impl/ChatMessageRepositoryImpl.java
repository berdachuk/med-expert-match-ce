package com.berdachuk.medexpertmatch.chat.repository.impl;

import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import com.berdachuk.medexpertmatch.chat.repository.ChatMessageRepository;
import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ChatMessageMapper chatMessageMapper;

    @InjectSql("/sql/chat/getNextSequenceNumber.sql")
    private String getNextSequenceNumberSql;

    @InjectSql("/sql/chat/saveMessage.sql")
    private String saveMessageSql;

    @InjectSql("/sql/chat/getHistory.sql")
    private String getHistorySql;

    @InjectSql("/sql/chat/softDeleteMessagesByChatId.sql")
    private String softDeleteMessagesByChatIdSql;

    public ChatMessageRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            ChatMessageMapper chatMessageMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public ChatMessage saveMessage(String chatId, String role, String content) {
        Integer nextSeq = namedJdbcTemplate.queryForObject(
                getNextSequenceNumberSql,
                new MapSqlParameterSource("chatId", chatId),
                Integer.class);
        int sequenceNumber = nextSeq != null ? nextSeq : 1;
        String id = IdGenerator.generateId();
        Instant now = Instant.now();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("chatId", chatId)
                .addValue("role", role)
                .addValue("content", content)
                .addValue("sequenceNumber", sequenceNumber)
                .addValue("tokensUsed", null)
                .addValue("createdAt", Timestamp.from(now));
        namedJdbcTemplate.update(saveMessageSql, params);
        return new ChatMessage(id, chatId, role, content, sequenceNumber, null, now);
    }

    @Override
    public List<ChatMessage> getHistory(String chatId, int limit, int offset) {
        return namedJdbcTemplate.query(
                getHistorySql,
                new MapSqlParameterSource("chatId", chatId).addValue("limit", limit).addValue("offset", offset),
                chatMessageMapper);
    }

    @Override
    public int softDeleteByChatId(String chatId) {
        return namedJdbcTemplate.update(
                softDeleteMessagesByChatIdSql,
                new MapSqlParameterSource("chatId", chatId));
    }
}
