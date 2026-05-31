package com.berdachuk.medexpertmatch.llm.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;

/**
 * Applies turn-safe session memory with branch isolation for chat orchestration.
 */
public final class SessionAdvisorSupport {

    private SessionAdvisorSupport() {}

    public static void applyOrchestratorContext(ChatClient.AdvisorSpec spec, String sessionId) {
        spec.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId);
        spec.param(SessionMemoryAdvisor.EVENT_FILTER_CONTEXT_KEY,
                EventFilter.forBranch(AgentSessionBranches.ORCHESTRATOR));
    }
}
