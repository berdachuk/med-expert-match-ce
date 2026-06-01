package com.berdachuk.medexpertmatch.llm.harness;

import java.time.Instant;

public record CaseAnalysisCompletedEvent(String caseId, String sessionId, Instant completedAt) {
}
