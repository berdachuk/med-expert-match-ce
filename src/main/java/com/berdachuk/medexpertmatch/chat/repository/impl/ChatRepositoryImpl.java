package com.berdachuk.medexpertmatch.chat.repository.impl;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.repository.ChatRepository;
import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class ChatRepositoryImpl implements ChatRepository {

    private static final String DEFAULT_AGENT = "auto";

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ChatMapper chatMapper;

    @InjectSql("/sql/chat/create.sql")
    private String createSql;

    @InjectSql("/sql/chat/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/chat/findDefaultChat.sql")
    private String findDefaultChatSql;

    @InjectSql("/sql/chat/findAllByUserId.sql")
    private String findAllByUserIdSql;

    @InjectSql("/sql/chat/updateChatName.sql")
    private String updateChatNameSql;

    @InjectSql("/sql/chat/updateAgentId.sql")
    private String updateAgentIdSql;

    @InjectSql("/sql/chat/deleteChat.sql")
    private String deleteChatSql;

    @InjectSql("/sql/chat/updateLastActivity.sql")
    private String updateLastActivitySql;

    @InjectSql("/sql/chat/findIdleNonDefaultChats.sql")
    private String findIdleNonDefaultChatsSql;

    public ChatRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate, ChatMapper chatMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.chatMapper = chatMapper;
    }

    @Override
    public Chat createChat(String userId, String name, String agentId, boolean isDefault) {
        String id = IdGenerator.generateId();
        Instant now = Instant.now();
        String resolvedAgent = agentId != null && !agentId.isBlank() ? agentId : DEFAULT_AGENT;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId)
                .addValue("name", name != null ? name : "New Chat")
                .addValue("agentId", resolvedAgent)
                .addValue("isDefault", isDefault)
                .addValue("createdAt", Timestamp.from(now))
                .addValue("updatedAt", Timestamp.from(now))
                .addValue("lastActivityAt", Timestamp.from(now))
                .addValue("messageCount", 0);
        namedJdbcTemplate.update(createSql, params);
        return findById(id).orElseThrow();
    }

    @Override
    public Optional<Chat> findById(String chatId) {
        List<Chat> results = namedJdbcTemplate.query(
                findByIdSql, new MapSqlParameterSource("id", chatId), chatMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public Optional<Chat> findDefaultChat(String userId) {
        List<Chat> results = namedJdbcTemplate.query(
                findDefaultChatSql, new MapSqlParameterSource("userId", userId), chatMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public List<Chat> findAllByUserId(String userId) {
        return namedJdbcTemplate.query(
                findAllByUserIdSql, new MapSqlParameterSource("userId", userId), chatMapper);
    }

    @Override
    public boolean updateChatName(String chatId, String name) {
        int updated = namedJdbcTemplate.update(
                updateChatNameSql,
                new MapSqlParameterSource("id", chatId)
                        .addValue("name", name)
                        .addValue("updatedAt", Timestamp.from(Instant.now())));
        return updated > 0;
    }

    @Override
    public boolean updateAgentId(String chatId, String agentId) {
        String resolved = agentId != null && !agentId.isBlank() ? agentId : DEFAULT_AGENT;
        int updated = namedJdbcTemplate.update(
                updateAgentIdSql,
                new MapSqlParameterSource("id", chatId)
                        .addValue("agentId", resolved)
                        .addValue("updatedAt", Timestamp.from(Instant.now())));
        return updated > 0;
    }

    @Override
    public boolean deleteChat(String chatId) {
        return namedJdbcTemplate.update(deleteChatSql, new MapSqlParameterSource("id", chatId)) > 0;
    }

    @Override
    public void updateLastActivity(String chatId) {
        Instant now = Instant.now();
        namedJdbcTemplate.update(
                updateLastActivitySql,
                new MapSqlParameterSource("id", chatId)
                        .addValue("lastActivityAt", Timestamp.from(now))
                        .addValue("updatedAt", Timestamp.from(now)));
    }

    @Override
    public List<Chat> findIdleNonDefaultChatsBefore(Instant cutoff, int limit) {
        return namedJdbcTemplate.query(
                findIdleNonDefaultChatsSql,
                new MapSqlParameterSource("cutoff", Timestamp.from(cutoff)).addValue("limit", limit),
                chatMapper);
    }
}
