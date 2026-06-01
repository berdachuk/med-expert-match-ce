package com.berdachuk.medexpertmatch.llm.harness;

import java.time.Instant;

public record DoctorMatchCompletedEvent(String caseId, String sessionId, Instant completedAt) {
}
