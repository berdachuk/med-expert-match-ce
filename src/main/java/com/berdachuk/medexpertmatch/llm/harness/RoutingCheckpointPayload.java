package com.berdachuk.medexpertmatch.llm.harness;

import java.util.List;
import java.util.Map;

public record RoutingCheckpointPayload(
        String caseId,
        String sessionId,
        int maxResults,
        List<com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch> matches,
        String caseAnalysisJson,
        int bundleSectionCount) {
}
