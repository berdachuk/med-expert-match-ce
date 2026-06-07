package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;

/**
 * Inputs for the medical confidence policy router (M61).
 */
public record ConfidencePolicyInput(
        int matchCount,
        double topMatchScore,
        boolean verificationPassed,
        UrgencyLevel urgencyLevel,
        GoalType goalType,
        boolean insufficientGrounding,
        boolean operatorDisplayOverride) {

    public ConfidencePolicyInput {
        if (urgencyLevel == null) {
            urgencyLevel = UrgencyLevel.MEDIUM;
        }
        if (goalType == null) {
            goalType = GoalType.MATCH_DOCTORS;
        }
    }

    public ConfidencePolicyInput(
            int matchCount,
            double topMatchScore,
            boolean verificationPassed,
            UrgencyLevel urgencyLevel,
            GoalType goalType,
            boolean insufficientGrounding) {
        this(matchCount, topMatchScore, verificationPassed, urgencyLevel, goalType, insufficientGrounding, false);
    }
}
