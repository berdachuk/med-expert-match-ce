package com.berdachuk.medexpertmatch.llm.harness;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class HarnessWorkflowRunJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public HarnessWorkflowRunJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(HarnessWorkflowRun run) {
        jdbcTemplate.update("""
                INSERT INTO medexpertmatch.llm_harness_workflow_run
                    (run_id, session_id, case_id, workflow_type, state, resume_token, payload_json, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
                ON CONFLICT (run_id) DO UPDATE SET
                    state = EXCLUDED.state,
                    resume_token = EXCLUDED.resume_token,
                    payload_json = EXCLUDED.payload_json,
                    updated_at = EXCLUDED.updated_at
                """,
                run.runId(),
                run.sessionId(),
                run.caseId(),
                run.workflowType().name(),
                run.state().name(),
                run.resumeToken(),
                run.payloadJson(),
                Timestamp.from(run.createdAt()),
                Timestamp.from(run.updatedAt()));
    }

    public void updateState(String runId, DoctorMatchWorkflowState state) {
        jdbcTemplate.update("""
                UPDATE medexpertmatch.llm_harness_workflow_run
                SET state = ?, updated_at = CURRENT_TIMESTAMP
                WHERE run_id = ?
                """, state.name(), runId);
    }

    public Optional<HarnessWorkflowRun> findById(String runId) {
        return jdbcTemplate.query("""
                        SELECT run_id, session_id, case_id, workflow_type, state, resume_token, payload_json, created_at, updated_at
                        FROM medexpertmatch.llm_harness_workflow_run
                        WHERE run_id = ?
                        """,
                rowMapper(),
                runId).stream().findFirst();
    }

    public static String newRunId() {
        return UUID.randomUUID().toString();
    }

    public static String newResumeToken() {
        return UUID.randomUUID().toString();
    }

    private static RowMapper<HarnessWorkflowRun> rowMapper() {
        return (rs, rowNum) -> new HarnessWorkflowRun(
                rs.getString("run_id"),
                rs.getString("session_id"),
                rs.getString("case_id"),
                HarnessWorkflowType.valueOf(rs.getString("workflow_type")),
                DoctorMatchWorkflowState.valueOf(rs.getString("state")),
                rs.getString("resume_token"),
                rs.getString("payload_json"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
