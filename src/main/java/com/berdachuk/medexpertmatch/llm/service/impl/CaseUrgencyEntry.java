package com.berdachuk.medexpertmatch.llm.service.impl;

/**
 * Holds parsed urgency and specialty for a case, used for queue prioritization sort.
 */
public record CaseUrgencyEntry(String caseId, String urgencyLevel, String specialty) {
}
