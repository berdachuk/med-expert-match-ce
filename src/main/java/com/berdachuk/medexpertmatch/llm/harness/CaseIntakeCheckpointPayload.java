package com.berdachuk.medexpertmatch.llm.harness;

import java.util.Map;

public record CaseIntakeCheckpointPayload(
        String caseId,
        String sessionId,
        String caseText,
        Map<String, Object> request) {
}
