package com.berdachuk.medexpertmatch.llm.agent;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import reactor.core.publisher.Flux;

/**
 * Injects subagent session id and branch filter into the advisor context before
 * {@link SessionMemoryAdvisor} loads branch-scoped history.
 */
public final class SubagentSessionContextAdvisor implements CallAdvisor, StreamAdvisor {

    private final String agentId;

    public SubagentSessionContextAdvisor(String agentId) {
        this.agentId = agentId;
    }

    @Override
    public String getName() {
        return "subagentSessionContextAdvisor-" + agentId;
    }

    @Override
    public int getOrder() {
        return ToolCallingAdvisor.DEFAULT_ORDER;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        return chain.nextCall(augmentRequest(request));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(augmentRequest(request));
    }

    private ChatClientRequest augmentRequest(ChatClientRequest request) {
        String sessionId = OrchestrationContextHolder.sessionIdOrNull();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "default";
        }
        return request.mutate()
                .context(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId)
                .context(SessionMemoryAdvisor.EVENT_FILTER_CONTEXT_KEY,
                        AgentSessionBranches.subagentEventFilter(agentId))
                .build();
    }
}
