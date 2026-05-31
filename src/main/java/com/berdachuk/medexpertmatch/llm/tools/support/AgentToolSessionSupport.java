package com.berdachuk.medexpertmatch.llm.tools.support;

import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;

/**
 * Resolves the current orchestration session id for agent tool invocations.
 */
public final class AgentToolSessionSupport {

    private AgentToolSessionSupport() {
    }

    public static String getSessionId() {
        return OrchestrationContextHolder.sessionIdOrNull();
    }
}
