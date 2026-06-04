package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
class HarnessRetentionRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.getJdbcTemplate().execute("DELETE FROM medexpertmatch.llm_harness_chain_event");
        jdbc.getJdbcTemplate().execute("DELETE FROM medexpertmatch.llm_harness_workflow_run");
    }

    @Test
    @DisplayName("deleteChainEventsOlderThan removes only old chain events")
    void deletesOldChainEvents() {
        Instant now = Instant.now();
        MapSqlParameterSource oldParams = new MapSqlParameterSource()
                .addValue("id", "old-event")
                .addValue("chainRootSessionId", "root-session-1")
                .addValue("sessionId", "session-1")
                .addValue("step", "TOOL_CALL")
                .addValue("createdAt", Timestamp.from(now.minus(100, ChronoUnit.DAYS)));
        jdbc.update("INSERT INTO medexpertmatch.llm_harness_chain_event (id, chain_root_session_id, session_id, step, created_at) VALUES (:id, :chainRootSessionId, :sessionId, :step, :createdAt)", oldParams);

        MapSqlParameterSource recentParams = new MapSqlParameterSource()
                .addValue("id", "recent-event")
                .addValue("chainRootSessionId", "root-session-2")
                .addValue("sessionId", "session-2")
                .addValue("step", "TOOL_CALL")
                .addValue("createdAt", Timestamp.from(now));
        jdbc.update("INSERT INTO medexpertmatch.llm_harness_chain_event (id, chain_root_session_id, session_id, step, created_at) VALUES (:id, :chainRootSessionId, :sessionId, :step, :createdAt)", recentParams);

        Instant cutoff = now.minus(60, ChronoUnit.DAYS);
        int deleted = jdbc.update(
                "DELETE FROM medexpertmatch.llm_harness_chain_event WHERE created_at < :cutoff",
                new MapSqlParameterSource("cutoff", Timestamp.from(cutoff)));

        assertEquals(1, deleted, "Should delete only the old event");

        Long remaining = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM medexpertmatch.llm_harness_chain_event", Long.class);
        assertEquals(1, remaining);
    }

    @Test
    @DisplayName("deleteWorkflowRunsOlderThan removes old runs except NEEDS_HUMAN")
    void deletesOldWorkflowRunsExceptHuman() {
        Instant now = Instant.now();

        jdbc.update("INSERT INTO medexpertmatch.llm_harness_workflow_run (run_id, session_id, workflow_type, state, resume_token, payload_json, created_at, updated_at) VALUES (:runId, :sessionId, :workflowType, :state, :resumeToken, CAST(:payloadJson AS jsonb), :createdAt, :updatedAt)",
                new MapSqlParameterSource()
                        .addValue("runId", "old-run")
                        .addValue("sessionId", "session-old")
                        .addValue("workflowType", "DOCTOR_MATCH")
                        .addValue("state", DoctorMatchWorkflowState.DONE.name())
                        .addValue("resumeToken", "token-old")
                        .addValue("payloadJson", "{}")
                        .addValue("createdAt", Timestamp.from(now.minus(100, ChronoUnit.DAYS)))
                        .addValue("updatedAt", Timestamp.from(now.minus(100, ChronoUnit.DAYS))));

        jdbc.update("INSERT INTO medexpertmatch.llm_harness_workflow_run (run_id, session_id, workflow_type, state, resume_token, payload_json, created_at, updated_at) VALUES (:runId, :sessionId, :workflowType, :state, :resumeToken, CAST(:payloadJson AS jsonb), :createdAt, :updatedAt)",
                new MapSqlParameterSource()
                        .addValue("runId", "human-run")
                        .addValue("sessionId", "session-human")
                        .addValue("workflowType", "DOCTOR_MATCH")
                        .addValue("state", DoctorMatchWorkflowState.NEEDS_HUMAN.name())
                        .addValue("resumeToken", "token-human")
                        .addValue("payloadJson", "{}")
                        .addValue("createdAt", Timestamp.from(now.minus(100, ChronoUnit.DAYS)))
                        .addValue("updatedAt", Timestamp.from(now.minus(100, ChronoUnit.DAYS))));

        Instant cutoff = now.minus(60, ChronoUnit.DAYS);
        int deleted = jdbc.update(
                "DELETE FROM medexpertmatch.llm_harness_workflow_run WHERE updated_at < :cutoff AND state != :needsHumanState",
                new MapSqlParameterSource()
                        .addValue("cutoff", Timestamp.from(cutoff))
                        .addValue("needsHumanState", DoctorMatchWorkflowState.NEEDS_HUMAN.name()));

        assertEquals(1, deleted, "Should delete old DONE run but NOT the NEEDS_HUMAN run");

        Long remaining = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM medexpertmatch.llm_harness_workflow_run", Long.class);
        assertEquals(1, remaining);
    }
}
