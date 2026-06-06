package com.berdachuk.medexpertmatch.llm.chat;

import java.util.Optional;

/**
 * Result of classifying a user's request into a high-level goal.
 * Includes the detected goal type and any extracted case ID for routing.
 */
public record GoalClassification(
        GoalType goalType,
        Optional<String> caseId,
        Optional<String> matchId,
        String summary
) {
    public static GoalClassification general() {
        return new GoalClassification(GoalType.GENERAL_QUESTION, Optional.empty(), Optional.empty(), "general question");
    }

    public static GoalClassification matchDoctors(String caseId, String summary) {
        return new GoalClassification(GoalType.MATCH_DOCTORS, optionalCaseId(caseId), Optional.empty(), summary);
    }

    public static GoalClassification analyzeCase(String caseId, String summary) {
        return new GoalClassification(GoalType.ANALYZE_CASE, optionalCaseId(caseId), Optional.empty(), summary);
    }

    public static GoalClassification routeCase(String caseId, String summary) {
        return new GoalClassification(GoalType.ROUTE_CASE, optionalCaseId(caseId), Optional.empty(), summary);
    }

    private static Optional<String> optionalCaseId(String caseId) {
        if (caseId == null || caseId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(caseId.trim());
    }

    public static GoalClassification triageIntake(String summary) {
        return new GoalClassification(GoalType.TRIAGE_INTAKE, Optional.empty(), Optional.empty(), summary);
    }

    public static GoalClassification searchEvidence(String summary) {
        return new GoalClassification(GoalType.SEARCH_EVIDENCE, Optional.empty(), Optional.empty(), summary);
    }

    public static GoalClassification generateRecommendations(String matchId, String summary) {
        return new GoalClassification(GoalType.GENERATE_RECOMMENDATIONS, Optional.empty(), Optional.of(matchId), summary);
    }

    public boolean isRoutableToEngine() {
        return goalType == GoalType.MATCH_DOCTORS || goalType == GoalType.ROUTE_CASE;
    }

    public boolean isAnalyzableViaHarness() {
        return goalType == GoalType.ANALYZE_CASE && hasCaseId();
    }

    public boolean hasCaseId() {
        return caseId.filter(id -> !id.isBlank()).isPresent();
    }
}
