package com.berdachuk.medexpertmatch.llm.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.session.EventFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SessionBranchIsolationTest {

    @Test
    @DisplayName("Orchestrator branch constant is defined for session isolation")
    void orchestratorBranchConstant() {
        assertEquals("orch", AgentSessionBranches.ORCHESTRATOR);
    }

    @Test
    @DisplayName("EventFilter.forBranch binds orchestrator branch name")
    void eventFilterForOrchestratorBranch() {
        EventFilter filter = EventFilter.forBranch(AgentSessionBranches.ORCHESTRATOR);
        assertNotNull(filter);
        assertEquals(AgentSessionBranches.ORCHESTRATOR, filter.branch());
    }
}
