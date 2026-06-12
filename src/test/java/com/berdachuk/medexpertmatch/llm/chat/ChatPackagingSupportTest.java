package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.core.config.LlmTierProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatPackagingSupportTest {

    private final LlmTierProperties tiers = new LlmTierProperties(
            new LlmTierProperties.TierBudget(2048),
            new LlmTierProperties.TierBudget(4096),
            new LlmTierProperties.TierBudget(6000));

    @Test
    @DisplayName("EXPERT_MATCH uses harness for routable goals")
    void expertMatchUsesHarness() {
        GoalClassification goal = GoalClassification.matchDoctors("case123", "test");
        assertTrue(ChatPackagingSupport.shouldUseHarness(goal, ChatInteractionMode.EXPERT_MATCH));
    }

    @Test
    @DisplayName("relative cost hint reflects tier token budgets")
    void relativeCostHint() {
        assertTrue(ChatPackagingSupport.relativeCostHint(ChatInteractionMode.EXPERT_MATCH, tiers).contains("6000"));
    }

    @Test
    @DisplayName("parse defaults to expert match")
    void parseMode() {
        assertEquals(ChatInteractionMode.EXPERT_MATCH, ChatInteractionMode.parse(null));
        assertEquals(ChatInteractionMode.EXPERT_MATCH, ChatInteractionMode.parse("quick"));
        assertEquals(ChatInteractionMode.EXPERT_MATCH, ChatInteractionMode.parse("harness"));
    }
}