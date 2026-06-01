package com.berdachuk.medexpertmatch.llm.harness;

import java.util.Optional;

public interface HarnessWorkflowRunStore {

    void save(HarnessWorkflowRun run);

    Optional<HarnessWorkflowRun> findById(String runId);

    java.util.List<HarnessWorkflowRun> findByState(DoctorMatchWorkflowState state, int limit);

    void updateState(String runId, DoctorMatchWorkflowState state);
}
