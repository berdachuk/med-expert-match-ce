package com.berdachuk.medexpertmatch.llm.automemory;

/**
 * Decides when the long-term memory layer should consolidate accumulated activity into durable
 * memory. Implementations are stateful (they observe agent activity) and cheap (no LLM calls).
 */
public interface MemoryConsolidationTrigger {

    /**
     * Records that the agent did something worth consolidating later (e.g. completed a turn). Resets
     * any time/turn-based window.
     */
    void recordActivity();

    /**
     * @return {@code true} when the configured condition (e.g. idle gap elapsed) is met and
     * consolidation should run.
     */
    boolean shouldConsolidate();
}
