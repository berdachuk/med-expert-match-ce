package com.berdachuk.medexpertmatch.llm.event;

import com.berdachuk.medexpertmatch.llm.metrics.PipelineMetricsService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventRetryService {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 200;

    private final EventDeadLetterQueue deadLetterQueue;
    private final PipelineMetricsService pipelineMetrics;

    public EventRetryService(EventDeadLetterQueue deadLetterQueue, PipelineMetricsService pipelineMetrics) {
        this.deadLetterQueue = deadLetterQueue;
        this.pipelineMetrics = pipelineMetrics;
    }

    public <T> T executeWithRetry(String sessionId, String agentName, String eventType,
                                   RetryableSupplier<T> action) throws Exception {
        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastError = e;
                log.warn("Agent {} failed attempt {}/{} session={}: {}",
                        agentName, attempt, MAX_RETRIES, sessionId, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    long backoff = BASE_BACKOFF_MS * (1L << (attempt - 1));
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        String errorMsg = lastError != null ? lastError.getMessage() : "Unknown error";
        pipelineMetrics.recordStageFailed(sessionId, agentName, errorMsg);
        deadLetterQueue.enqueue(sessionId, agentName, eventType, errorMsg, "");
        log.error("Agent {} exhausted retries session={}, sent to DLQ", agentName, sessionId);
        throw lastError;
    }

    public void replayDeadLetter(String deadLetterId) {
        DeadLetterEvent event = deadLetterQueue.findById(deadLetterId);
        if (event == null) {
            throw new IllegalArgumentException("Dead letter not found: " + deadLetterId);
        }
        deadLetterQueue.removeById(deadLetterId);
        log.info("Replaying dead letter {} session={} agent={}", deadLetterId, event.sessionId(), event.agentName());
    }

    @FunctionalInterface
    public interface RetryableSupplier<T> {
        T get() throws Exception;
    }
}