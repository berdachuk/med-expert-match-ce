package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import org.springframework.ai.session.compaction.CompactionResult;
import org.springframework.ai.session.compaction.CompactionStrategy;

/**
 * Records compaction metrics/logs while delegating to the configured non-LLM strategy.
 */
public class ObservingCompactionStrategy implements CompactionStrategy {

    private final CompactionStrategy delegate;
    private final SessionCompactionObservability observability;

    public ObservingCompactionStrategy(CompactionStrategy delegate,
                                       SessionCompactionObservability observability) {
        this.delegate = delegate;
        this.observability = observability;
    }

    @Override
    public CompactionResult compact(org.springframework.ai.session.compaction.CompactionRequest request) {
        String sessionId = request.session() != null
                ? request.session().id()
                : OrchestrationContextHolder.sessionIdOrNull();
        try {
            CompactionResult result = delegate.compact(request);
            observability.recordCompaction(sessionId, result.eventsRemoved());
            return result;
        } catch (RuntimeException ex) {
            observability.recordFailure(sessionId);
            throw ex;
        }
    }
}
