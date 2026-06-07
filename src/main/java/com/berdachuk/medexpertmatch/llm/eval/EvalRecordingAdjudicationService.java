package com.berdachuk.medexpertmatch.llm.eval;

import com.berdachuk.medexpertmatch.llm.harness.HarnessAdjudicationEntry;
import com.berdachuk.medexpertmatch.llm.harness.HarnessAdjudicationService;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowCheckpointService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory adjudication audit log for deterministic eval (M69).
 */
final class EvalRecordingAdjudicationService implements HarnessAdjudicationService {

    private final List<HarnessAdjudicationEntry> entries = new ArrayList<>();

    @Override
    public HarnessAdjudicationEntry record(
            String runId,
            String caseId,
            String reviewerId,
            HarnessWorkflowCheckpointService.CheckpointAction decision,
            String comment) {
        HarnessAdjudicationEntry entry = new HarnessAdjudicationEntry(
                "eval-" + entries.size(),
                runId,
                caseId,
                reviewerId,
                decision.name(),
                comment,
                Instant.now());
        entries.add(entry);
        return entry;
    }

    @Override
    public List<HarnessAdjudicationEntry> listRecent(int limit) {
        int from = Math.max(0, entries.size() - limit);
        return List.copyOf(entries.subList(from, entries.size()));
    }

    HarnessAdjudicationEntry lastEntry() {
        return entries.isEmpty() ? null : entries.getLast();
    }
}
