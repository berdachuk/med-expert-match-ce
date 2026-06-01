package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.llm.harness.impl.JdbcAgentPlanArtefactStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentPlanArtefactStoreIT extends BaseIntegrationTest {

    @Autowired
    private JdbcAgentPlanArtefactStore planArtefactStore;

    @Test
    @DisplayName("JDBC plan store persists and reloads artefact by session id")
    void saveAndFind() {
        String sessionId = "harness-plan-it-" + System.nanoTime();
        AgentPlanArtefact plan = new AgentPlanArtefact(
                sessionId,
                HarnessWorkflowType.DOCTOR_MATCH,
                "6a1c68963a08e800010de68e",
                List.of("step-one", "step-two"),
                List.of("disclaimer present"),
                Instant.now());

        planArtefactStore.save(plan);
        Optional<AgentPlanArtefact> loaded = planArtefactStore.findBySessionId(sessionId);

        assertTrue(loaded.isPresent());
        assertEquals(2, loaded.get().steps().size());
        assertEquals(HarnessWorkflowType.DOCTOR_MATCH, loaded.get().workflowType());
    }
}
