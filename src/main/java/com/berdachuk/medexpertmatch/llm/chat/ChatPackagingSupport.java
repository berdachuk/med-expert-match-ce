package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.core.config.LlmTierProperties;
import com.berdachuk.medexpertmatch.llm.routing.RoutingTier;

public final class ChatPackagingSupport {

    private ChatPackagingSupport() {}

    public static boolean shouldUseHarness(GoalClassification goal, ChatInteractionMode mode) {
        return goal.isRoutableToEngine() && goal.hasCaseId();
    }

    public static boolean shouldUseCaseAnalysisHarness(
            GoalClassification goal, ChatInteractionMode mode, boolean analyzeCaseHarnessEnabled) {
        return analyzeCaseHarnessEnabled && goal.isAnalyzableViaHarness();
    }

    public static RoutingTier effectiveRoutingTier(GoalClassification goal, ChatInteractionMode mode) {
        return com.berdachuk.medexpertmatch.llm.routing.RoutingTierResolver.fromClassification(goal);
    }

    public static String modeHint(ChatInteractionMode mode) {
        return "User selected Expert match (harness) mode — use GraphRAG tools and structured matching when appropriate.\n\n";
    }

    public static String relativeCostHint(ChatInteractionMode mode, LlmTierProperties tierProperties) {
        int full = tierProperties.full().maxTokens();
        return String.format("~2–3× token budget (FULL harness + GraphRAG, up to %d tokens)", full);
    }
}