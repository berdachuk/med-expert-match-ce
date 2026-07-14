package com.berdachuk.medexpertmatch.llm.agent;

import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.compaction.CompactionResult;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.CompactionTrigger;

import java.time.Instant;
import java.util.List;

/**
 * Post-filters orchestrator branch reads so {@code orch.sub.*} subagent tool transcripts stay
 * out of orchestrator short-term memory while subagents retain ancestor visibility via their own filters.
 */
public final class OrchestratorBranchSessionService implements SessionService {

    private final SessionService delegate;

    public OrchestratorBranchSessionService(SessionService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Session create(CreateSessionRequest request) {
        return delegate.create(request);
    }

    @Override
    public Session findById(String sessionId) {
        return delegate.findById(sessionId);
    }

    @Override
    public List<Session> findByUserId(String userId) {
        return delegate.findByUserId(userId);
    }

    @Override
    public void delete(String sessionId) {
        delegate.delete(sessionId);
    }

    @Override
    public int deleteExpiredSessions(Instant cutoff) {
        return delegate.deleteExpiredSessions(cutoff);
    }

    @Override
    public void appendEvent(SessionEvent event) {
        delegate.appendEvent(event);
    }

    @Override
    public List<SessionEvent> getEvents(String sessionId, EventFilter filter) {
        List<SessionEvent> events = delegate.getEvents(sessionId, filter);
        if (filter != null && AgentSessionBranches.ORCHESTRATOR.equals(filter.branch())) {
            return events.stream().filter(AgentSessionBranches::isOrchestratorScopedEvent).toList();
        }
        return events;
    }

    @Override
    public CompactionResult compact(String sessionId, CompactionTrigger trigger, CompactionStrategy strategy) {
        return delegate.compact(sessionId, trigger, strategy);
    }
}
