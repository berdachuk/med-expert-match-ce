package com.berdachuk.medexpertmatch.llm.eval;

import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcome;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel;
import com.berdachuk.medexpertmatch.retrieval.service.MatchOutcomeService;

import java.time.Instant;

/**
 * In-memory outcome recorder for deterministic adjudication eval (M69).
 */
final class EvalRecordingMatchOutcomeService implements MatchOutcomeService {

    private String lastCaseId;
    private String lastDoctorId;
    private MatchOutcomeLabel lastLabel;

    @Override
    public MatchOutcome recordOutcome(String caseId, String doctorId, MatchOutcomeLabel label) {
        lastCaseId = caseId;
        lastDoctorId = doctorId;
        lastLabel = label;
        return new MatchOutcome("eval-outcome", caseId, doctorId, label, Instant.now());
    }

    String lastCaseId() {
        return lastCaseId;
    }

    String lastDoctorId() {
        return lastDoctorId;
    }

    MatchOutcomeLabel lastLabel() {
        return lastLabel;
    }
}
