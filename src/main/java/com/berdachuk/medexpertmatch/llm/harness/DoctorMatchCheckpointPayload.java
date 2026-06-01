package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;

import java.util.List;

public record DoctorMatchCheckpointPayload(
        String caseId,
        String sessionId,
        int maxResults,
        List<DoctorMatch> matches,
        String caseAnalysisJson,
        int bundleSectionCount) {
}
