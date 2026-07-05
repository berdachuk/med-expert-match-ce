package com.berdachuk.medexpertmatch.caseanalysis.domain;

import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;

/**
 * Structured urgency classification output from the LLM.
 */
public record UrgencyClassificationJson(String level) {

    public UrgencyLevel toUrgencyLevel() {
        if (level == null || level.isBlank()) {
            throw new IllegalArgumentException("Missing urgency level in structured output");
        }
        return UrgencyLevel.valueOf(level.trim().toUpperCase());
    }
}
