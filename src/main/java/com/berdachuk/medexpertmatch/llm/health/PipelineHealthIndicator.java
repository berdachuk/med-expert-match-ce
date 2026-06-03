package com.berdachuk.medexpertmatch.llm.health;

import com.berdachuk.medexpertmatch.llm.event.EventDeadLetterQueue;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Component
public class PipelineHealthIndicator implements HealthIndicator {

    private final MeterRegistry meterRegistry;
    private final EventDeadLetterQueue deadLetterQueue;

    public PipelineHealthIndicator(MeterRegistry meterRegistry, EventDeadLetterQueue deadLetterQueue) {
        this.meterRegistry = meterRegistry;
        this.deadLetterQueue = deadLetterQueue;
    }

    @Override
    public Health health() {
        int dlqSize = deadLetterQueue.size();
        double failureRate = 0;
        try {
            double completed = meterRegistry.get("pipeline.stage.completed").counter().count();
            double failed = meterRegistry.get("pipeline.stage.failed").counter().count();
            double total = completed + failed;
            failureRate = total > 0 ? failed / total : 0;
        } catch (Exception ignored) {}

        var detail = new LinkedHashMap<String, Object>();
        detail.put("deadLetterQueueSize", dlqSize);
        detail.put("stageFailureRate", failureRate);

        if (dlqSize > 50 || failureRate > 0.5) {
            return Health.down().withDetails(detail).build();
        }
        if (dlqSize > 0 || failureRate > 0.1) {
            return Health.status("WARN").withDetails(detail).build();
        }
        return Health.up().withDetails(detail).build();
    }
}