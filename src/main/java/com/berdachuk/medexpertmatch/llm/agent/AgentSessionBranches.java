package com.berdachuk.medexpertmatch.llm.agent;

import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;

/**
 * Session branch names for orchestrator vs subagent lineage isolation (M15 / M141).
 * <p>
 * Subagents use {@code orch.sub.{agentId}} branches. The stock {@link EventFilter#forBranch(String)}
 * treats {@code orch} as an ancestor of {@code orch.sub.*}, so orchestrator reads are post-filtered
 * via {@link OrchestratorBranchSessionService} to keep subagent tool transcripts out of orchestrator memory.
 */
public final class AgentSessionBranches {

    public static final String ORCHESTRATOR = "orch";
    public static final String SUBAGENT_PREFIX = "orch.sub.";

    private AgentSessionBranches() {}

    public static String subagentBranch(String agentId) {
        return SUBAGENT_PREFIX + normalizeAgentId(agentId);
    }

    public static String agentIdFromFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        return filename.endsWith(".md") ? filename.substring(0, filename.length() - 3) : filename;
    }

    public static EventFilter orchestratorEventFilter() {
        return EventFilter.forBranch(ORCHESTRATOR);
    }

    public static EventFilter subagentEventFilter(String agentId) {
        return EventFilter.forBranch(subagentBranch(agentId));
    }

    public static boolean isOrchestratorScopedEvent(SessionEvent event) {
        String branch = event.getBranch();
        return branch == null || ORCHESTRATOR.equals(branch);
    }

    private static String normalizeAgentId(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return "unknown";
        }
        String normalized = agentId;
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash < normalized.length() - 1) {
            normalized = normalized.substring(slash + 1);
        }
        return agentIdFromFilename(normalized);
    }
}
