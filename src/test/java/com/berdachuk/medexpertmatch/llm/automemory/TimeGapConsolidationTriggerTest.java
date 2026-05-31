package com.berdachuk.medexpertmatch.llm.automemory;

import com.berdachuk.medexpertmatch.llm.config.AgentMemoryProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the time-gap consolidation predicate: it fires only once the configured idle gap has
 * elapsed since the last recorded activity, and stays quiet within the gap. A controllable clock
 * keeps the test deterministic (no real sleeping).
 */
class TimeGapConsolidationTriggerTest {

    private static AgentMemoryProperties props(long gapSeconds) {
        return new AgentMemoryProperties(
                "/tmp/ignored",
                new AgentMemoryProperties.Consolidation(gapSeconds, null, null));
    }

    @Test
    @DisplayName("Returns false within the configured gap, true once the gap elapses")
    void firesAfterGap() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-05-30T10:00:00Z"));
        TimeGapConsolidationTrigger trigger = TimeGapConsolidationTrigger.withClock(props(60L), now::get);

        // record activity at t0
        trigger.recordActivity();

        // 30s later — still within the 60s gap
        now.set(Instant.parse("2026-05-30T10:00:30Z"));
        assertFalse(trigger.shouldConsolidate(), "should NOT consolidate within the gap");

        // 61s later — gap elapsed
        now.set(Instant.parse("2026-05-30T10:01:01Z"));
        assertTrue(trigger.shouldConsolidate(), "should consolidate after the gap elapses");
    }

    @Test
    @DisplayName("Exactly at the gap boundary it has not yet fired")
    void boundaryIsExclusive() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-05-30T10:00:00Z"));
        TimeGapConsolidationTrigger trigger = TimeGapConsolidationTrigger.withClock(props(60L), now::get);
        trigger.recordActivity();

        now.set(now.get().plus(Duration.ofSeconds(60)));
        assertFalse(trigger.shouldConsolidate(), "at exactly the gap the trigger stays quiet");

        now.set(now.get().plus(Duration.ofSeconds(1)));
        assertTrue(trigger.shouldConsolidate(), "one second past the gap it fires");
    }

    @Test
    @DisplayName("recordActivity resets the gap window")
    void recordActivityResets() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-05-30T10:00:00Z"));
        TimeGapConsolidationTrigger trigger = TimeGapConsolidationTrigger.withClock(props(60L), now::get);
        trigger.recordActivity();

        now.set(Instant.parse("2026-05-30T10:02:00Z"));
        assertTrue(trigger.shouldConsolidate(), "gap elapsed");

        // fresh activity resets the window
        trigger.recordActivity();
        assertFalse(trigger.shouldConsolidate(), "after new activity the gap restarts");
    }
}
