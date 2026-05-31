package com.berdachuk.medexpertmatch.llm.automemory;

import com.berdachuk.medexpertmatch.llm.config.AgentMemoryProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Time-gap consolidation predicate: fires once the configured idle gap has elapsed since the last
 * recorded activity. The gap is configurable via {@code agent.memory.consolidation.gap-seconds}.
 * <p>
 * The boundary is exclusive — exactly at the gap it stays quiet, one tick past it fires — so that
 * consolidation only runs after a genuine pause in activity. A {@link Supplier}&lt;{@link Instant}&gt;
 * clock is injected so the predicate is deterministically unit-testable without real sleeping.
 */
@Slf4j
public class TimeGapConsolidationTrigger implements MemoryConsolidationTrigger {

    private final Duration gap;
    private final Supplier<Instant> clock;
    private final AtomicReference<Instant> lastActivity = new AtomicReference<>();

    public TimeGapConsolidationTrigger(AgentMemoryProperties properties) {
        this(properties, Instant::now);
    }

    private TimeGapConsolidationTrigger(AgentMemoryProperties properties, Supplier<Instant> clock) {
        this.gap = Duration.ofSeconds(properties.consolidation().gapSeconds());
        this.clock = clock;
        this.lastActivity.set(clock.get());
    }

    static TimeGapConsolidationTrigger withClock(AgentMemoryProperties properties, Supplier<Instant> clock) {
        return new TimeGapConsolidationTrigger(properties, clock);
    }

    @Override
    public void recordActivity() {
        lastActivity.set(clock.get());
    }

    @Override
    public boolean shouldConsolidate() {
        Instant last = lastActivity.get();
        if (last == null) {
            return false;
        }
        Duration elapsed = Duration.between(last, clock.get());
        return elapsed.compareTo(gap) > 0;
    }
}
