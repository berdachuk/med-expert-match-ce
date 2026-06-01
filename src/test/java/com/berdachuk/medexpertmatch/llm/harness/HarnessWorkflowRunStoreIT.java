package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessWorkflowRunStoreIT extends BaseIntegrationTest {

    @Autowired
    private HarnessWorkflowRunStore workflowRunStore;

    @Test
    @DisplayName("persists and loads harness workflow runs")
    void persistsRun() {
        String runId = HarnessWorkflowRunJdbcRepository.newRunId();
        HarnessWorkflowRun run = new HarnessWorkflowRun(
                runId,
                "it-session",
                "6a1c68963a08e800010de68e",
                HarnessWorkflowType.DOCTOR_MATCH,
                DoctorMatchWorkflowState.NEEDS_HUMAN,
                HarnessWorkflowRunJdbcRepository.newResumeToken(),
                "{\"caseId\":\"6a1c68963a08e800010de68e\",\"sessionId\":\"it-session\",\"maxResults\":5,\"matches\":[],\"caseAnalysisJson\":\"{}\",\"bundleSectionCount\":0}",
                Instant.now(),
                Instant.now());
        workflowRunStore.save(run);

        HarnessWorkflowRun loaded = workflowRunStore.findById(runId).orElseThrow();
        assertEquals(DoctorMatchWorkflowState.NEEDS_HUMAN, loaded.state());
        assertTrue(loaded.payloadJson().contains("it-session"));
    }
}
