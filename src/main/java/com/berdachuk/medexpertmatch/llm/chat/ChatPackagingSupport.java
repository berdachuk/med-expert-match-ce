package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.core.config.LlmTierProperties;
import com.berdachuk.medexpertmatch.llm.routing.RoutingTier;

/**
 * Agent vs chat product packaging helpers (M66).
 */
public final class ChatPackagingSupport {

    private ChatPackagingSupport() {}

    public static boolean shouldUseHarness(GoalClassification goal, ChatInteractionMode mode) {
        return mode == ChatInteractionMode.EXPERT_MATCH
                && goal.isRoutableToEngine()
                && goal.hasCaseId();
    }

    public static boolean shouldUseCaseAnalysisHarness(
            GoalClassification goal, ChatInteractionMode mode, boolean analyzeCaseHarnessEnabled) {
        return mode == ChatInteractionMode.EXPERT_MATCH
                && analyzeCaseHarnessEnabled
                && goal.isAnalyzableViaHarness();
    }

    public static RoutingTier effectiveRoutingTier(GoalClassification goal, ChatInteractionMode mode) {
        if (mode == ChatInteractionMode.QUICK) {
            return RoutingTier.LIGHT;
        }
        return com.berdachuk.medexpertmatch.llm.routing.RoutingTierResolver.fromClassification(goal);
    }

    public static String modeHint(ChatInteractionMode mode) {
        return mode == ChatInteractionMode.QUICK
                ? "User selected Quick question mode — answer concisely via chat tools; do not invoke full harness workflows.\n\n"
                : "User selected Expert match (harness) mode — use GraphRAG tools and structured matching when appropriate.\n\n";
    }

    public static String relativeCostHint(ChatInteractionMode mode, LlmTierProperties tierProperties) {
        int light = tierProperties.light().maxTokens();
        int full = tierProperties.full().maxTokens();
        double ratio = light > 0 ? (double) full / light : 2.0;
        return mode == ChatInteractionMode.QUICK
                ? "~1× token budget (LIGHT, up to " + light + " tokens)"
                : String.format("~%.0f× token budget (FULL harness + GraphRAG, up to %d tokens)", ratio, full);
    }
}
