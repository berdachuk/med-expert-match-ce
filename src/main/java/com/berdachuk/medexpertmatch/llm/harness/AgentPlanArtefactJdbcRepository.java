package com.berdachuk.medexpertmatch.llm.harness;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
public class AgentPlanArtefactJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AgentPlanArtefactJdbcRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void upsert(AgentPlanArtefact artefact) {
        try {
            String stepsJson = objectMapper.writeValueAsString(artefact.steps());
            String acceptanceJson = objectMapper.writeValueAsString(artefact.acceptanceCriteria());
            jdbc.update("""
                    INSERT INTO medexpertmatch.llm_agent_plan_artefact
                        (session_id, workflow_type, case_id, steps_json, acceptance_criteria_json, created_at)
                    VALUES (:sessionId, :workflowType, :caseId, :steps::jsonb, :acceptance::jsonb, :createdAt)
                    ON CONFLICT (session_id) DO UPDATE SET
                        workflow_type = EXCLUDED.workflow_type,
                        case_id = EXCLUDED.case_id,
                        steps_json = EXCLUDED.steps_json,
                        acceptance_criteria_json = EXCLUDED.acceptance_criteria_json,
                        created_at = EXCLUDED.created_at
                    """,
                    Map.of(
                            "sessionId", artefact.sessionId(),
                            "workflowType", artefact.workflowType().name(),
                            "caseId", artefact.caseId(),
                            "steps", stepsJson,
                            "acceptance", acceptanceJson,
                            "createdAt", Timestamp.from(artefact.createdAt())));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist agent plan artefact", e);
        }
    }

    public Optional<AgentPlanArtefact> findBySessionId(String sessionId) {
        List<AgentPlanArtefact> results = jdbc.query(
                """
                        SELECT session_id, workflow_type, case_id, steps_json::text, acceptance_criteria_json::text, created_at
                        FROM medexpertmatch.llm_agent_plan_artefact
                        WHERE session_id = :sessionId
                        """,
                Map.of("sessionId", sessionId),
                (rs, rowNum) -> mapRow(rs));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private AgentPlanArtefact mapRow(ResultSet rs) throws SQLException {
        try {
            List<String> steps = objectMapper.readValue(rs.getString("steps_json"), new TypeReference<>() {});
            List<String> acceptance = objectMapper.readValue(rs.getString("acceptance_criteria_json"), new TypeReference<>() {});
            return new AgentPlanArtefact(
                    rs.getString("session_id"),
                    HarnessWorkflowType.valueOf(rs.getString("workflow_type")),
                    rs.getString("case_id"),
                    steps,
                    acceptance,
                    rs.getTimestamp("created_at").toInstant());
        } catch (Exception e) {
            throw new SQLException("Failed to map agent plan artefact row", e);
        }
    }
}
