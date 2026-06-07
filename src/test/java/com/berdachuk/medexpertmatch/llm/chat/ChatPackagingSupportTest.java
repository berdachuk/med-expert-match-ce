package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.core.config.LlmTierProperties;
import com.berdachuk.medexpertmatch.llm.routing.RoutingTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPackagingSupportTest {

    private final LlmTierProperties tiers = new LlmTierProperties(
            new LlmTierProperties.TierBudget(2048),
            new LlmTierProperties.TierBudget(4096),
            new LlmTierProperties.TierBudget(6000));

    @Test
    @DisplayName("QUICK mode skips harness for routable goals")
    void quickModeSkipsHarness() {
        GoalClassification goal = GoalClassification.matchDoctors("case123", "test");
        assertFalse(ChatPackagingSupport.shouldUseHarness(goal, ChatInteractionMode.QUICK));
        assertTrue(ChatPackagingSupport.shouldUseHarness(goal, ChatInteractionMode.EXPERT_MATCH));
    }

    @Test
    @DisplayName("QUICK mode forces LIGHT routing tier")
    void quickModeForcesLightTier() {
        GoalClassification goal = GoalClassification.matchDoctors("case123", "test");
        assertEquals(RoutingTier.LIGHT, ChatPackagingSupport.effectiveRoutingTier(goal, ChatInteractionMode.QUICK));
        assertEquals(RoutingTier.FULL, ChatPackagingSupport.effectiveRoutingTier(goal, ChatInteractionMode.EXPERT_MATCH));
    }

    @Test
    @DisplayName("relative cost hint reflects tier token budgets")
    void relativeCostHint() {
        assertTrue(ChatPackagingSupport.relativeCostHint(ChatInteractionMode.QUICK, tiers).contains("2048"));
        assertTrue(ChatPackagingSupport.relativeCostHint(ChatInteractionMode.EXPERT_MATCH, tiers).contains("6000"));
    }

    @Test
    @DisplayName("parse defaults unknown values to expert match")
    void parseMode() {
        assertEquals(ChatInteractionMode.QUICK, ChatInteractionMode.parse("quick"));
        assertEquals(ChatInteractionMode.EXPERT_MATCH, ChatInteractionMode.parse(null));
        assertEquals(ChatInteractionMode.EXPERT_MATCH, ChatInteractionMode.parse("harness"));
    }
}
