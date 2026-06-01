package com.berdachuk.medexpertmatch.llm.harness.impl;

import com.berdachuk.medexpertmatch.llm.harness.DoctorMatchWorkflowState;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRun;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRunStore;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryHarnessWorkflowRunStore implements HarnessWorkflowRunStore {

    private final Map<String, HarnessWorkflowRun> runs = new ConcurrentHashMap<>();

    @Override
    public void save(HarnessWorkflowRun run) {
        runs.put(run.runId(), run);
    }

    @Override
    public Optional<HarnessWorkflowRun> findById(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    @Override
    public void updateState(String runId, DoctorMatchWorkflowState state) {
        HarnessWorkflowRun existing = runs.get(runId);
        if (existing == null) {
            return;
        }
        runs.put(runId, new HarnessWorkflowRun(
                existing.runId(),
                existing.sessionId(),
                existing.caseId(),
                existing.workflowType(),
                state,
                existing.resumeToken(),
                existing.payloadJson(),
                existing.createdAt(),
                Instant.now()));
    }
}
