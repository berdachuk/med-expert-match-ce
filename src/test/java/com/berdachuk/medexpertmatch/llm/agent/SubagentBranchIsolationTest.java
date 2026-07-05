package com.berdachuk.medexpertmatch.llm.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubagentBranchIsolationTest {

    @Test
    @DisplayName("subagentBranch derives orch.sub.{agentId} from agent id")
    void subagentBranchNaming() {
        assertEquals("orch.sub.case-analyzer", AgentSessionBranches.subagentBranch("case-analyzer"));
        assertEquals("orch.sub.routing-planner",
                AgentSessionBranches.subagentBranch("classpath:/agents/routing-planner.md"));
    }

    @Test
    @DisplayName("agentIdFromFilename strips .md suffix")
    void agentIdFromFilename() {
        assertEquals("case-analyzer", AgentSessionBranches.agentIdFromFilename("case-analyzer.md"));
    }

    @Test
    @DisplayName("Subagent branch events are excluded from orchestrator visibility predicate")
    void subagentEventsExcludedFromOrchestratorScope() {
        SessionEvent subagentEvent = sessionEvent("orch.sub.test", "subagent tool output");
        SessionEvent orchestratorEvent = sessionEvent(AgentSessionBranches.ORCHESTRATOR, "orchestrator reply");
        SessionEvent rootEvent = sessionEvent(null, "shared user turn");

        assertFalse(AgentSessionBranches.isOrchestratorScopedEvent(subagentEvent));
        assertTrue(AgentSessionBranches.isOrchestratorScopedEvent(orchestratorEvent));
        assertTrue(AgentSessionBranches.isOrchestratorScopedEvent(rootEvent));
    }

    @Test
    @DisplayName("Orchestrator event filter excludes orch.sub.* sibling branches via session service")
    void orchestratorFilterExcludesSubagentBranches() {
        SessionService delegate = mock(SessionService.class);
        SessionEvent subagentEvent = sessionEvent("orch.sub.test", "hidden");
        SessionEvent orchestratorEvent = sessionEvent(AgentSessionBranches.ORCHESTRATOR, "visible");
        when(delegate.getEvents(eq("session-1"), any(EventFilter.class)))
                .thenReturn(java.util.List.of(subagentEvent, orchestratorEvent));

        OrchestratorBranchSessionService service = new OrchestratorBranchSessionService(delegate);
        var events = service.getEvents("session-1", AgentSessionBranches.orchestratorEventFilter());

        assertEquals(1, events.size());
        assertTrue(AgentSessionBranches.isOrchestratorScopedEvent(events.getFirst()));
    }

    @Test
    @DisplayName("Subagent event filter includes root and orchestrator ancestor events")
    void subagentFilterIncludesAncestors() {
        EventFilter filter = AgentSessionBranches.subagentEventFilter("case-analyzer");

        assertTrue(filter.matches(sessionEvent(null, "root user message")));
        assertTrue(filter.matches(sessionEvent(AgentSessionBranches.ORCHESTRATOR, "orchestrator plan")));
        assertTrue(filter.matches(sessionEvent("orch.sub.case-analyzer", "subagent work")));
        assertFalse(filter.matches(sessionEvent("orch.sub.routing-planner", "sibling subagent")));
    }

    private static SessionEvent sessionEvent(String branch, String text) {
        SessionEvent.Builder builder = SessionEvent.builder()
                .sessionId("session-1")
                .timestamp(Instant.now())
                .message(new UserMessage(text));
        if (branch != null) {
            builder.branch(branch);
        }
        return builder.build();
    }
}
