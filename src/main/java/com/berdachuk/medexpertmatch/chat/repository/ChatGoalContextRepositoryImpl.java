package com.berdachuk.medexpertmatch.chat.repository;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class ChatGoalContextRepositoryImpl {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @com.berdachuk.medexpertmatch.core.repository.sql.InjectSql("/sql/chat/upsertGoalContext.sql")
    private String upsertGoalContextSql;

    @com.berdachuk.medexpertmatch.core.repository.sql.InjectSql("/sql/chat/findGoalContext.sql")
    private String findGoalContextSql;

    @com.berdachuk.medexpertmatch.core.repository.sql.InjectSql("/sql/chat/deleteGoalContext.sql")
    private String deleteGoalContextSql;

    @com.berdachuk.medexpertmatch.core.repository.sql.InjectSql("/sql/chat/deleteGoalContextByChat.sql")
    private String deleteGoalContextByChatSql;

    public ChatGoalContextRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    public void upsert(String sessionId, String goalType, String caseId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("sessionId", sessionId)
                .addValue("goalType", goalType)
                .addValue("caseId", caseId)
                .addValue("updatedAt", Timestamp.from(Instant.now()));
        namedJdbcTemplate.update(upsertGoalContextSql, params);
    }

    public Optional<ChatGoalContextRow> findBySessionId(String sessionId) {
        List<ChatGoalContextRow> results = namedJdbcTemplate.query(
                findGoalContextSql,
                new MapSqlParameterSource("sessionId", sessionId),
                (rs, rowNum) -> new ChatGoalContextRow(
                        rs.getString("session_id"),
                        rs.getString("goal_type"),
                        rs.getString("case_id")));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void deleteBySessionId(String sessionId) {
        namedJdbcTemplate.update(deleteGoalContextSql,
                new MapSqlParameterSource("sessionId", sessionId));
    }

    public int deleteByChatPattern(String chatPattern) {
        return namedJdbcTemplate.update(deleteGoalContextByChatSql,
                new MapSqlParameterSource("chatPattern", chatPattern));
    }

    public record ChatGoalContextRow(String sessionId, String goalType, String caseId) {}
}
